"""
Differential testing harness for original vs obfuscated EVM bytecode.

Key properties:
- Reproducible input generation (seeded RNG)
- ABI selector-level invocation logging (JSONL)
- Observable comparisons: return values (for eth_call), event logs (tx receipts),
  and selected storage slots (eth_getStorageAt)
- Per-contract summary JSON for coverage and statistics

"""

from __future__ import annotations

import argparse
import dataclasses
import json
import os
import random
import sys
import time
from dataclasses import dataclass
from decimal import Decimal
from typing import Any, Dict, List, Optional, Sequence, Tuple

from eth_abi import encode as abi_encode
from eth_utils import keccak, to_bytes, to_checksum_address
from web3 import Web3
from web3.contract import Contract
from web3.exceptions import ContractLogicError, TransactionNotFound


def die(msg: str, code: int = 1) -> None:
    print(f"[FATAL] {msg}", file=sys.stderr)
    sys.exit(code)

def ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)

def json_dumps(obj: Any) -> str:
    return json.dumps(obj, ensure_ascii=False, sort_keys=True, default=str)

def now_ts() -> str:
    return time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())

def load_json(path: str) -> Dict[str, Any]:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def selector_from_sig(sig: str) -> str:
    # 4-byte selector hex string
    return "0x" + keccak(text=sig).hex()[:8]

def solidity_sig(fn_abi: Dict[str, Any]) -> str:
    # e.g., transfer(address,uint256)
    name = fn_abi["name"]
    inputs = fn_abi.get("inputs", [])
    types = ",".join(i["type"] for i in inputs)
    return f"{name}({types})"

def stable_hash_logs(logs: Sequence[Dict[str, Any]]) -> str:
    """
    Create a stable hash of logs for comparison (topics+data+address).
    """
    norm = []
    for lg in logs:
        norm.append({
            "address": lg.get("address"),
            "topics": [t.hex() if hasattr(t, "hex") else str(t) for t in lg.get("topics", [])],
            "data": lg.get("data"),
        })
    b = json.dumps(norm, sort_keys=True).encode("utf-8")
    return "0x" + keccak(b).hex()

def hex_int(s: str) -> int:
    s = s.strip().lower()
    if s.startswith("0x"):
        return int(s, 16)
    return int(s, 10)


def is_int_type(t: str) -> bool:
    return t.startswith("uint") or t.startswith("int")

def int_bitwidth(t: str) -> int:
    # uint256 -> 256, int8 -> 8; default 256 if no suffix.
    digits = "".join(ch for ch in t if ch.isdigit())
    return int(digits) if digits else 256

def is_bytes_type(t: str) -> bool:
    return t == "bytes" or t.startswith("bytes")

def bytes_len(t: str) -> Optional[int]:
    # bytes32 -> 32, bytes -> None
    if t == "bytes":
        return None
    digits = "".join(ch for ch in t if ch.isdigit())
    return int(digits) if digits else None

def gen_boundary_int(t: str, rng: random.Random) -> int:
    bw = int_bitwidth(t)
    signed = t.startswith("int")
    if signed:
        # signed range: [-2^(bw-1), 2^(bw-1)-1]
        mn = -(1 << (bw - 1))
        mx = (1 << (bw - 1)) - 1
        candidates = [0, 1, -1, mn, mx]
    else:
        mn = 0
        mx = (1 << bw) - 1
        candidates = [0, 1, mx]
    return rng.choice(candidates)

def gen_random_int(t: str, rng: random.Random) -> int:
    bw = int_bitwidth(t)
    signed = t.startswith("int")
    if signed:
        mn = -(1 << (bw - 1))
        mx = (1 << (bw - 1)) - 1
        return rng.randint(mn, mx)
    return rng.randint(0, (1 << bw) - 1)

def gen_address(rng: random.Random, accounts: List[str]) -> str:
    # Prefer existing accounts for practical executability
    if accounts:
        return Web3.to_checksum_address(rng.choice(accounts))
    # fallback random address
    b = bytes(rng.getrandbits(8) for _ in range(20))
    return Web3.to_checksum_address("0x" + b.hex())

def gen_bool(rng: random.Random) -> bool:
    return bool(rng.getrandbits(1))

def gen_bytes(t: str, rng: random.Random, boundary: bool) -> bytes:
    ln = bytes_len(t)
    if ln is None:
        # dynamic bytes: choose a small length deterministically
        ln = 0 if boundary else rng.randint(0, 64)
    if boundary:
        # boundary-like: all-zero or all-0xff
        return rng.choice([b"\x00" * ln, b"\xff" * ln])
    return bytes(rng.getrandbits(8) for _ in range(ln))

def gen_string(rng: random.Random, boundary: bool) -> str:
    if boundary:
        return rng.choice(["", "a", "test"])
    # deterministic but varied
    n = rng.randint(0, 32)
    alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
    return "".join(rng.choice(alphabet) for _ in range(n))

def gen_value_for_type(
    t: str,
    rng: random.Random,
    label: str,
    accounts: List[str],
    structured_hint: Optional[Any] = None,
) -> Any:
    boundary = (label == "boundary")
    structured = (label == "structured")

    # Arrays (very basic support): e.g., uint256[] or uint256[3]
    if t.endswith("]"):
        # parse base and length
        base = t[:t.index("[")]
        inside = t[t.index("[")+1:t.index("]")]
        fixed_len = int(inside) if inside.isdigit() else None
        if fixed_len is None:
            # dynamic: small length
            ln = 0 if boundary else rng.randint(0, 5)
        else:
            ln = fixed_len
        arr = []
        for i in range(ln):
            arr.append(gen_value_for_type(base, rng, label, accounts, structured_hint=i))
        return arr

    if is_int_type(t):
        if boundary:
            return gen_boundary_int(t, rng)
        if structured and structured_hint is not None:
            # structured: simple pattern based on index
            if isinstance(structured_hint, int):
                return structured_hint
        return gen_random_int(t, rng)

    if t == "address":
        return gen_address(rng, accounts)

    if t == "bool":
        if boundary:
            return rng.choice([False, True])
        return gen_bool(rng)

    if is_bytes_type(t):
        return gen_bytes(t, rng, boundary=boundary)

    if t == "string":
        return gen_string(rng, boundary=boundary)

    # Unsupported types (tuples, complex structs): return None -> caller may skip
    return None

def choose_label(rng: random.Random, boundary_ratio: float, structured_ratio: float) -> str:
    x = rng.random()
    if x < boundary_ratio:
        return "boundary"
    if x < boundary_ratio + structured_ratio:
        return "structured"
    return "random"

def wait_receipt(w3: Web3, tx_hash: str, timeout: int = 120) -> Dict[str, Any]:
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            r = w3.eth.get_transaction_receipt(tx_hash)
            if r is not None:
                return dict(r)
        except TransactionNotFound:
            pass
        time.sleep(0.5)
    raise TimeoutError(f"Timeout waiting for receipt: {tx_hash}")

def supports_method(w3: Web3, method: str) -> bool:
    try:
        # Some clients support "rpc_modules" only; method presence isn't guaranteed.
        _ = w3.provider.make_request("rpc_modules", [])
        return True
    except Exception:
        return False

def get_fee_fields(w3: Web3, gas_price_wei: Optional[int]) -> Dict[str, int]:
    if gas_price_wei is not None:
        return {"gasPrice": int(gas_price_wei)}

    # Try EIP-1559 style
    try:
        prio = w3.eth.max_priority_fee  # may raise
        base = w3.eth.get_block("latest").get("baseFeePerGas", 0)
        # conservative max fee
        max_fee = int(base) + int(prio) * 2
        return {"maxPriorityFeePerGas": int(prio), "maxFeePerGas": int(max_fee)}
    except Exception:
        # fallback legacy
        gp = int(w3.eth.gas_price)
        return {"gasPrice": gp}

def normalize_call_result(x: Any) -> Any:
    if isinstance(x, (bytes, bytearray)):
        return "0x" + bytes(x).hex()
    if isinstance(x, (int, str, bool)) or x is None:
        return x
    if isinstance(x, (list, tuple)):
        return [normalize_call_result(v) for v in x]
    # web3 returns AttributeDict sometimes
    try:
        return json.loads(json.dumps(x, default=str))
    except Exception:
        return str(x)

def read_storage_slots(w3: Web3, addr: str, slots: List[int]) -> Dict[str, str]:
    out: Dict[str, str] = {}
    for s in slots:
        v = w3.eth.get_storage_at(addr, s)
        out[str(s)] = "0x" + v.hex()
    return out

@dataclass
class Artifact:
    abi: List[Dict[str, Any]]
    bytecode: str

def load_artifact(path: str) -> Artifact:
    j = load_json(path)
    abi = j.get("abi")
    bytecode = j.get("bytecode") or j.get("evm", {}).get("bytecode", {}).get("object")
    if abi is None or bytecode is None:
        raise ValueError(f"Artifact missing 'abi' or 'bytecode': {path}")
    if not isinstance(bytecode, str):
        raise ValueError("bytecode must be a hex string")
    if not bytecode.startswith("0x"):
        bytecode = "0x" + bytecode
    return Artifact(abi=abi, bytecode=bytecode)

def deploy_contract(
    w3: Web3,
    artifact: Artifact,
    sender: str,
    gas: int,
    gas_price_wei: Optional[int],
    value_wei: int,
    timeout: int,
) -> Tuple[str, str]:
    contract = w3.eth.contract(abi=artifact.abi, bytecode=artifact.bytecode)
    tx = contract.constructor().build_transaction({
        "from": sender,
        "nonce": w3.eth.get_transaction_count(sender),
        "gas": int(gas),
        "value": int(value_wei),
        **get_fee_fields(w3, gas_price_wei),
    })
    # If node has unlocked account, send_transaction works.
    tx_hash = w3.eth.send_transaction(tx)
    if hasattr(tx_hash, "hex"):
        tx_hash = tx_hash.hex()
    receipt = wait_receipt(w3, tx_hash, timeout=timeout)
    addr = receipt.get("contractAddress")
    if not addr:
        raise RuntimeError(f"Deployment failed, receipt: {receipt}")
    return Web3.to_checksum_address(addr), tx_hash

def iter_function_abis(abi: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    fns = [x for x in abi if x.get("type") == "function"]
    # Stable ordering by signature for reproducibility
    fns.sort(key=lambda x: solidity_sig(x))
    return fns

def is_view_like(fn_abi: Dict[str, Any]) -> bool:
    sm = fn_abi.get("stateMutability", "")
    return sm in ("view", "pure")

def can_execute(fn_abi: Dict[str, Any]) -> bool:
    # Exclude functions with tuple/complex inputs to keep harness robust
    for inp in fn_abi.get("inputs", []):
        t = inp.get("type", "")
        if t.startswith("tuple"):
            return False
    return True

def build_args_for_fn(
    fn_abi: Dict[str, Any],
    rng: random.Random,
    label: str,
    accounts: List[str],
) -> Optional[List[Any]]:
    args: List[Any] = []
    inputs = fn_abi.get("inputs", [])
    for idx, inp in enumerate(inputs):
        t = inp["type"]
        v = gen_value_for_type(t, rng, label, accounts, structured_hint=idx)
        if v is None:
            return None
        args.append(v)
    return args

def compare_observables(
    orig: Dict[str, Any],
    obf: Dict[str, Any],
) -> Tuple[bool, List[str]]:
    diffs: List[str] = []
    keys = ["status", "return", "logs_hash", "storage"]
    for k in keys:
        if orig.get(k) != obf.get(k):
            diffs.append(k)
    return (len(diffs) == 0), diffs

def main() -> None:
    ap = argparse.ArgumentParser(description="Differential testing: original vs obfuscated EVM bytecode")
    ap.add_argument("--rpc", required=True, help="JSON-RPC URL, e.g., http://127.0.0.1:8545")
    ap.add_argument("--orig", required=True, help="Path to original artifact JSON (abi+bytecode)")
    ap.add_argument("--obf", required=True, help="Path to obfuscated artifact JSON (abi+bytecode)")
    ap.add_argument("--outdir", required=True, help="Output directory for logs and summaries")
    ap.add_argument("--seed", type=int, default=20250101, help="Random seed for reproducibility")
    ap.add_argument("--calls-per-func", type=int, default=32, help="Number of test calls per ABI function")
    ap.add_argument("--boundary-ratio", type=float, default=0.43, help="Fraction of boundary-labeled inputs")
    ap.add_argument("--structured-ratio", type=float, default=0.10, help="Fraction of structured-labeled inputs")
    ap.add_argument("--storage-slots", default="", help="Comma-separated storage slot indices (e.g., 0,1,2 or 0x0,0x1)")
    ap.add_argument("--gas", type=int, default=3_000_000, help="Gas limit for tx/deploy")
    ap.add_argument("--gas-price", default="", help="Legacy gasPrice in wei (optional). If omitted, uses node defaults/EIP-1559.")
    ap.add_argument("--value-wei", type=int, default=0, help="Value (wei) for constructor/tx if needed")
    ap.add_argument("--timeout", type=int, default=120, help="RPC/receipt timeout seconds")
    args = ap.parse_args()

    if not (0.0 <= args.boundary_ratio <= 1.0):
        die("--boundary-ratio must be in [0,1]")
    if not (0.0 <= args.structured_ratio <= 1.0):
        die("--structured-ratio must be in [0,1]")
    if args.boundary_ratio + args.structured_ratio > 1.0:
        die("boundary_ratio + structured_ratio must be <= 1.0")

    slots: List[int] = []
    if args.storage_slots.strip():
        for part in args.storage_slots.split(","):
            part = part.strip()
            if part:
                slots.append(hex_int(part))

    gas_price_wei: Optional[int] = None
    if args.gas_price.strip():
        gas_price_wei = int(args.gas_price.strip())

    ensure_dir(args.outdir)
    inv_path = os.path.join(args.outdir, "invocations.jsonl")
    sum_path = os.path.join(args.outdir, "summary.json")
    dep_path = os.path.join(args.outdir, "deployed.json")

    w3 = Web3(Web3.HTTPProvider(args.rpc, request_kwargs={"timeout": args.timeout}))
    if not w3.is_connected():
        die(f"Cannot connect to RPC: {args.rpc}")

    accounts = list(getattr(w3.eth, "accounts", [])) or []
    if not accounts:
        die("No unlocked accounts found on node (w3.eth.accounts empty). Provide an unlocked account on Geth dev/private chain.")

    sender = Web3.to_checksum_address(accounts[0])

    orig_art = load_artifact(args.orig)
    obf_art = load_artifact(args.obf)

    # Basic ABI compatibility check (signatures should match)
    orig_fns = iter_function_abis(orig_art.abi)
    obf_fns = iter_function_abis(obf_art.abi)
    orig_sigs = [solidity_sig(f) for f in orig_fns]
    obf_sigs = [solidity_sig(f) for f in obf_fns]
    if orig_sigs != obf_sigs:
        die("ABI mismatch between original and obfuscated artifacts (function signatures differ).")

    print(f"[{now_ts()}] Connected: chainId={w3.eth.chain_id}, sender={sender}")
    print(f"[{now_ts()}] Deploying original contract ...")
    orig_addr, orig_tx = deploy_contract(w3, orig_art, sender, args.gas, gas_price_wei, args.value_wei, args.timeout)
    print(f"[{now_ts()}] Deploying obfuscated contract ...")
    obf_addr, obf_tx = deploy_contract(w3, obf_art, sender, args.gas, gas_price_wei, args.value_wei, args.timeout)
    print(f"[{now_ts()}] Deployed: orig={orig_addr}, obf={obf_addr}")

    with open(dep_path, "w", encoding="utf-8") as f:
        json.dump({
            "rpc": args.rpc,
            "chain_id": w3.eth.chain_id,
            "sender": sender,
            "orig": {"address": orig_addr, "deploy_tx": orig_tx},
            "obf": {"address": obf_addr, "deploy_tx": obf_tx},
            "seed": args.seed,
            "calls_per_func": args.calls_per_func,
            "boundary_ratio": args.boundary_ratio,
            "structured_ratio": args.structured_ratio,
            "storage_slots": slots,
            "created_at": now_ts(),
        }, f, ensure_ascii=False, indent=2)

    orig_contract: Contract = w3.eth.contract(address=orig_addr, abi=orig_art.abi)
    obf_contract: Contract = w3.eth.contract(address=obf_addr, abi=obf_art.abi)

    rng = random.Random(args.seed)

    tested_selectors: set[str] = set()
    total_selectors: set[str] = set(selector_from_sig(sig) for sig in orig_sigs)

    counts = {"boundary": 0, "structured": 0, "random": 0}
    mismatches = 0
    mismatch_contract = False
    total_calls = 0

    # JSONL writer
    inv_f = open(inv_path, "w", encoding="utf-8")

    def run_one(
        contract: Contract,
        fn_abi: Dict[str, Any],
        fn_sig: str,
        fn_selector: str,
        args_list: List[Any],
        label: str,
    ) -> Dict[str, Any]:
        view_like = is_view_like(fn_abi)
        out: Dict[str, Any] = {
            "status": None,
            "return": None,
            "logs_hash": None,
            "storage": None,
            "tx_hash": None,
            "block": None,
            "gas_used": None,
            "error": None,
        }
        try:
            fn = contract.get_function_by_signature(fn_sig)(*args_list)
        except Exception as e:
            out["status"] = "error"
            out["error"] = f"function_bind_error: {type(e).__name__}: {e}"
            return out

        if view_like:
            try:
                ret = fn.call({"from": sender})
                out["status"] = "ok"
                out["return"] = normalize_call_result(ret)
                out["logs_hash"] = "0x0"  # no logs for eth_call
                out["storage"] = read_storage_slots(w3, contract.address, slots) if slots else {}
            except ContractLogicError as e:
                out["status"] = "revert"
                out["error"] = str(e)
                out["return"] = None
                out["logs_hash"] = "0x0"
                out["storage"] = read_storage_slots(w3, contract.address, slots) if slots else {}
            except Exception as e:
                out["status"] = "error"
                out["error"] = f"{type(e).__name__}: {e}"
            return out

        # state-changing tx
        try:
            nonce = w3.eth.get_transaction_count(sender)
            tx = fn.build_transaction({
                "from": sender,
                "nonce": nonce,
                "gas": int(args.gas),
                "value": int(args.value_wei),
                **get_fee_fields(w3, gas_price_wei),
            })
            tx_hash = w3.eth.send_transaction(tx)
            if hasattr(tx_hash, "hex"):
                tx_hash = tx_hash.hex()
            receipt = wait_receipt(w3, tx_hash, timeout=args.timeout)
            out["tx_hash"] = tx_hash
            out["block"] = receipt.get("blockNumber")
            out["gas_used"] = receipt.get("gasUsed")
            status = receipt.get("status", 1)
            out["status"] = "ok" if status == 1 else "revert"
            logs = receipt.get("logs", []) or []
            out["logs_hash"] = stable_hash_logs(logs)
            # for tx we do not reliably have return values; record None
            out["return"] = None
            out["storage"] = read_storage_slots(w3, contract.address, slots) if slots else {}
        except ContractLogicError as e:
            out["status"] = "revert"
            out["error"] = str(e)
            out["logs_hash"] = "0x0"
            out["storage"] = read_storage_slots(w3, contract.address, slots) if slots else {}
        except Exception as e:
            out["status"] = "error"
            out["error"] = f"{type(e).__name__}: {e}"
        return out

    # Main loop: per function, generate calls-per-func testcases
    for fn_abi in orig_fns:
        fn_sig = solidity_sig(fn_abi)
        fn_selector = selector_from_sig(fn_sig)
        total_selectors.add(fn_selector)

        if not can_execute(fn_abi):
            # Log skip for transparency
            inv_f.write(json_dumps({
                "ts": now_ts(),
                "function": fn_sig,
                "selector": fn_selector,
                "skipped": True,
                "reason": "unsupported_input_type(tuple/complex)",
            }) + "\n")
            continue

        for k in range(args.calls_per_func):
            label = choose_label(rng, args.boundary_ratio, args.structured_ratio)
            args_list = build_args_for_fn(fn_abi, rng, label, accounts)
            if args_list is None:
                inv_f.write(json_dumps({
                    "ts": now_ts(),
                    "function": fn_sig,
                    "selector": fn_selector,
                    "skipped": True,
                    "reason": "arg_generation_failed(unsupported_type)",
                    "label": label,
                }) + "\n")
                continue

            counts[label] += 1
            total_calls += 1
            tested_selectors.add(fn_selector)

            orig_obs = run_one(orig_contract, fn_abi, fn_sig, fn_selector, args_list, label)
            obf_obs = run_one(obf_contract, fn_abi, fn_sig, fn_selector, args_list, label)

            ok, diff_fields = compare_observables(orig_obs, obf_obs)
            if not ok:
                mismatches += 1
                mismatch_contract = True

            inv_f.write(json_dumps({
                "ts": now_ts(),
                "contract_pair": {
                    "orig": orig_addr,
                    "obf": obf_addr,
                },
                "function": fn_sig,
                "selector": fn_selector,
                "label": label,
                "args": args_list,
                "mutability": fn_abi.get("stateMutability"),
                "orig": orig_obs,
                "obf": obf_obs,
                "mismatch": (not ok),
                "mismatch_fields": diff_fields,
            }) + "\n")

    inv_f.close()

    uncovered_selectors = sorted(list(total_selectors - tested_selectors))

    summary = {
        "created_at": now_ts(),
        "rpc": args.rpc,
        "chain_id": w3.eth.chain_id,
        "contract_pair": {"orig": orig_addr, "obf": obf_addr},
        "seed": args.seed,
        "calls_per_func": args.calls_per_func,
        "boundary_ratio_target": args.boundary_ratio,
        "structured_ratio_target": args.structured_ratio,
        "storage_slots": slots,
        "counts": counts,
        "total_calls": total_calls,
        "mismatches": mismatches,
        "contract_mismatch": mismatch_contract,
        "abi_functions_total": len(orig_sigs),
        "selectors_total": len(total_selectors),
        "selectors_tested": len(tested_selectors),
        "uncovered_selectors": uncovered_selectors,
        "function_coverage": (len(tested_selectors) / max(1, len(total_selectors))),
        "notes": [
            "Return-value comparison is performed for view/pure functions via eth_call.",
            "For state-changing tx, comparison uses (status, logs_hash, storage slots).",
            "This harness is reproducible via --seed and fixed call ordering.",
        ],
    }

    with open(sum_path, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)

    print(f"[{now_ts()}] Done.")
    print(f"  invocations: {inv_path}")
    print(f"  summary:     {sum_path}")
    print(f"  deployed:    {dep_path}")
    print(f"  total_calls={total_calls}, mismatches={mismatches}, func_cov={summary['function_coverage']:.4f}")
    if uncovered_selectors:
        print(f"  uncovered_selectors={len(uncovered_selectors)} (see summary.json)")

if __name__ == "__main__":
    main()
