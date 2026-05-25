"""
pip_helper.py — Pip operations for the Contrary Phone VPS terminal.
Provides dynamic runtime installation of pure-python wheels directly from PyPI.
"""
import sys
import os
import urllib.request
import json
import zipfile
import shutil
import traceback

# ── Dynamic Site-Packages Bootstrap ──────────────────────────────────────────
def get_site_packages_dir():
    try:
        from com.chaquo.python import Python
        context = Python.getInstance().getPlatform().getApplication()
        files_dir = context.getFilesDir().getAbsolutePath()
        site_packages = os.path.join(files_dir, "site-packages")
        if not os.path.exists(site_packages):
            os.makedirs(site_packages)
        return site_packages
    except Exception:
        # Fallback for non-Android environments
        fallback = os.path.join(os.path.expanduser("~"), ".contrary_vps_site_packages")
        if not os.path.exists(fallback):
            os.makedirs(fallback)
        return fallback

site_packages_dir = get_site_packages_dir()
if site_packages_dir not in sys.path:
    sys.path.append(site_packages_dir)


def _run_pip(*args):
    """Run a pip command. Since standard pip is missing at runtime, we emulate it."""
    cmd = args[0].lower() if args else ""
    if cmd == "install":
        # Handle install args
        pkg_specs = [a for a in args[1:] if not a.startswith("-")]
        if not pkg_specs:
            return (False, "Usage: pip install <package>")
        
        success_all = True
        output_all = ""
        for spec in pkg_specs:
            success, out = install_package(spec)
            success_all = success_all and success
            output_all += out + "\n"
        return (success_all, output_all)
        
    elif cmd == "uninstall":
        pkg_specs = [a for a in args[1:] if not a.startswith("-")]
        if not pkg_specs:
            return (False, "Usage: pip uninstall <package>")
            
        success_all = True
        output_all = ""
        for spec in pkg_specs:
            success, out = uninstall_package(spec)
            success_all = success_all and success
            output_all += out + "\n"
        return (success_all, output_all)
        
    elif cmd in ("list", "freeze"):
        packages = list_packages()
        return (True, "\n".join(packages))
        
    elif cmd == "show":
        pkg_specs = [a for a in args[1:] if not a.startswith("-")]
        if not pkg_specs:
            return (False, "Usage: pip show <package>")
        return (True, show_package(pkg_specs[0]))
        
    else:
        return (False, f"pip: unknown command '{cmd}'. Emulated pip supports: install, uninstall, list, show")


def install_package(package_spec: str):
    """
    Downloads and extracts a pure-python wheel from PyPI for package_spec (and its dependencies).
    """
    site_packages = get_site_packages_dir()
    
    # Parse package name and version (e.g. requests==2.31.0)
    parts = package_spec.split("==")
    pkg_name = parts[0].strip()
    pkg_ver = parts[1].strip() if len(parts) > 1 else None
    
    installed_in_run = set()
    
    def download_and_extract(name, version=None):
        name_lower = name.lower().replace("_", "-")
        if name_lower in installed_in_run:
            return True, f"{name} already processed"
            
        try:
            url = f"https://pypi.org/pypi/{name}/json"
            req = urllib.request.Request(url, headers={'User-Agent': 'ContraryVPS/1.0.0 (Android)'})
            with urllib.request.urlopen(req, timeout=10) as response:
                data = json.loads(response.read().decode())
            
            releases = data.get("releases", {})
            info = data.get("info", {})
            
            if not version:
                version = info.get("version")
                
            urls = releases.get(version, []) if version in releases else data.get("urls", [])
            
            # Find a pure Python wheel (py3-none-any.whl or py2.py3-none-any.whl)
            selected_url = None
            filename = ""
            for u in urls:
                fname = u.get("filename", "")
                if fname.endswith(".whl") and "none-any.whl" in fname:
                    selected_url = u.get("url")
                    filename = fname
                    break
                    
            # Fallback to any wheel
            if not selected_url:
                for u in urls:
                    fname = u.get("filename", "")
                    if fname.endswith(".whl"):
                        selected_url = u.get("url")
                        filename = fname
                        break
                        
            # Fallback to tar.gz/zip source dist
            if not selected_url:
                for u in urls:
                    fname = u.get("filename", "")
                    if fname.endswith(".tar.gz") or fname.endswith(".zip"):
                        selected_url = u.get("url")
                        filename = fname
                        break
                        
            if not selected_url:
                return False, f"No compatible package distribution found for {name}=={version}"
                
            # Download file
            temp_file = os.path.join(site_packages, f"temp_{name_lower}" + os.path.splitext(filename)[1])
            with urllib.request.urlopen(selected_url, timeout=15) as response, open(temp_file, 'wb') as out_file:
                shutil.copyfileobj(response, out_file)
                
            # Extract file
            if temp_file.endswith(".whl") or temp_file.endswith(".zip"):
                with zipfile.ZipFile(temp_file, 'r') as zip_ref:
                    zip_ref.extractall(site_packages)
            elif temp_file.endswith(".tar.gz"):
                import tarfile
                with tarfile.open(temp_file, 'r:gz') as tar_ref:
                    tar_ref.extractall(site_packages)
            
            # Clean up
            if os.path.exists(temp_file):
                os.remove(temp_file)
                
            installed_in_run.add(name_lower)
            output_msg = f"Successfully installed {name}=={version}"
            
            # Parse and install dependencies
            requires_dist = info.get("requires_dist") or []
            for req_str in requires_dist:
                if ";" in req_str:
                    # Filter out non-matching environment markers (e.g. sys_platform == 'win32')
                    # For simplicity, we ignore optional extra dependencies
                    if "extra ==" in req_str:
                        continue
                    req_str = req_str.split(";")[0].strip()
                if not req_str:
                    continue
                    
                dep_name = req_str.split()[0].strip()
                # Strip brackets, braces, comparisons
                dep_name = dep_name.replace("(", "").replace(")", "").replace("[", "").replace("]", "")
                dep_name_clean = dep_name.split("<")[0].split(">")[0].split("=")[0].strip()
                
                dep_name_lower = dep_name_clean.lower().replace("_", "-")
                if dep_name_lower not in installed_in_run:
                    # Check if already installed in site-packages
                    if not os.path.exists(os.path.join(site_packages, dep_name_clean)) and \
                       not os.path.exists(os.path.join(site_packages, dep_name_clean.replace("-", "_"))):
                        success, dep_msg = download_and_extract(dep_name_clean)
                        output_msg += "\n" + dep_msg
                        
            return True, output_msg
            
        except Exception as e:
            return False, f"Error installing {name}: {str(e)}"

    success, msg = download_and_extract(pkg_name, pkg_ver)
    return (success, msg)


def install_requirements(requirements_path: str):
    """Install packages from requirements.txt."""
    if not os.path.exists(requirements_path):
        return (False, f"requirements.txt not found at {requirements_path}")
        
    try:
        with open(requirements_path, "r", encoding="utf-8") as f:
            lines = f.readlines()
            
        success_all = True
        output_all = ""
        for line in lines:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            success, out = install_package(line)
            success_all = success_all and success
            output_all += out + "\n"
        return (success_all, output_all)
    except Exception as e:
        return (False, f"Failed to parse requirements.txt: {str(e)}")


def uninstall_package(package_name: str):
    """Uninstall a package by deleting its files in site-packages."""
    site_packages = get_site_packages_dir()
    pkg_clean = package_name.strip().lower().replace("-", "_")
    
    deleted_paths = []
    # Search for folder matching package name or package_name-version.dist-info
    for item in os.listdir(site_packages):
        item_lower = item.lower()
        if item_lower == pkg_clean or \
           item_lower.startswith(f"{pkg_clean}-") or \
           item_lower.startswith(f"{package_name.lower().replace('_', '-')}-"):
            path = os.path.join(site_packages, item)
            if os.path.isdir(path):
                shutil.rmtree(path)
            else:
                os.remove(path)
            deleted_paths.append(item)
            
    if deleted_paths:
        return (True, f"Uninstalled {package_name} (removed: {', '.join(deleted_paths)})")
    else:
        return (False, f"Package '{package_name}' not found in runtime site-packages.")


def list_packages():
    """Return list of installed packages as 'name==version' strings."""
    site_packages = get_site_packages_dir()
    packages = []
    
    # 1. Read build-time packages if available
    try:
        import pkg_resources
        for d in pkg_resources.working_set:
            packages.append(f"{d.project_name}=={d.version} (bundled)")
    except Exception:
        pass
        
    # 2. Read dynamically installed packages in site-packages
    try:
        for item in os.listdir(site_packages):
            if item.endswith(".dist-info") or item.endswith(".egg-info"):
                name_ver = item[:-10] if item.endswith(".dist-info") else item[:-9]
                if "-" in name_ver:
                    parts = name_ver.split("-")
                    name = parts[0]
                    ver = parts[1]
                    packages.append(f"{name}=={ver} (dynamic)")
    except Exception:
        pass
        
    return sorted(list(set(packages)))


def show_package(package_name: str) -> str:
    """Return show info for a package."""
    site_packages = get_site_packages_dir()
    pkg_clean = package_name.strip().lower().replace("-", "_")
    
    # Search in site-packages
    info_path = None
    for item in os.listdir(site_packages):
        item_lower = item.lower()
        if (item_lower.endswith(".dist-info") or item_lower.endswith(".egg-info")) and \
           (item_lower.startswith(f"{pkg_clean}-") or item_lower.startswith(f"{package_name.lower().replace('_', '-')}-")):
            info_path = os.path.join(site_packages, item)
            break
            
    if info_path:
        metadata_file = os.path.join(info_path, "METADATA")
        if os.path.exists(metadata_file):
            try:
                with open(metadata_file, "r", encoding="utf-8") as f:
                    return f.read()
            except Exception as e:
                return f"Error reading package metadata: {str(e)}"
                
    # Search in build-time packages
    try:
        import pkg_resources
        dist = pkg_resources.get_distribution(package_name)
        return f"Name: {dist.project_name}\nVersion: {dist.version}\nLocation: bundled\nRequires: {', '.join(str(r) for r in dist.requires())}"
    except Exception:
        pass
        
    return f"Package '{package_name}' not found."
