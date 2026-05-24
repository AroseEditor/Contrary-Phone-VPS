"""
pip_helper.py — Pip operations for the Contrary Phone VPS terminal.
Called from PipManager.kt via Chaquopy.
"""
import sys
import io
import traceback


def _run_pip(*args):
    """Run a pip command, capture output, return (success, output)."""
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    sys.stdout = buf = io.StringIO()
    sys.stderr = errbuf = io.StringIO()
    try:
        from pip._internal.cli.main import main as pip_main
        code = pip_main(list(args))
        out = buf.getvalue() + errbuf.getvalue()
        return (code == 0, out)
    except SystemExit as e:
        out = buf.getvalue() + errbuf.getvalue()
        return (e.code == 0, out)
    except Exception as e:
        return (False, traceback.format_exc())
    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr


def install_package(package_spec: str):
    """Install a package by name/spec. Returns (success, output)."""
    return _run_pip("install", "--upgrade", package_spec)


def install_requirements(requirements_path: str):
    """Install from requirements.txt. Returns (success, output)."""
    return _run_pip("install", "-r", requirements_path)


def uninstall_package(package_name: str):
    """Uninstall a package. Returns (success, output)."""
    return _run_pip("uninstall", "-y", package_name)


def list_packages():
    """Return list of installed packages as 'name==version' strings."""
    try:
        import pkg_resources
        return [f"{d.project_name}=={d.version}" for d in pkg_resources.working_set]
    except Exception as e:
        return [f"Error: {e}"]


def show_package(package_name: str) -> str:
    """Return pip show output for a package."""
    success, output = _run_pip("show", package_name)
    return output


def search_packages(query: str):
    """Return packages matching a query (local search only)."""
    try:
        import pkg_resources
        results = [
            f"{d.project_name}=={d.version}"
            for d in pkg_resources.working_set
            if query.lower() in d.project_name.lower()
        ]
        return results
    except Exception as e:
        return [f"Error: {e}"]
