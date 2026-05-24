"""
Terminal command dispatcher — handles interactive console commands.
Called from the Kotlin TerminalViewModel via PythonBridge.
"""
import sys
import io
import traceback
import subprocess as _subprocess


def exec_capture(code: str):
    """Execute Python code, capture stdout/stderr, return (stdout, stderr, exit_code)."""
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    sys.stdout = io.StringIO()
    sys.stderr = io.StringIO()
    exit_code = 0
    try:
        exec(compile(code, "<contrary-vps>", "exec"), {})
    except SystemExit as e:
        exit_code = e.code if isinstance(e.code, int) else 0
    except Exception:
        sys.stderr.write(traceback.format_exc())
        exit_code = 1
    finally:
        stdout_val = sys.stdout.getvalue()
        stderr_val = sys.stderr.getvalue()
        sys.stdout = old_stdout
        sys.stderr = old_stderr
    return (stdout_val, stderr_val, exit_code)
