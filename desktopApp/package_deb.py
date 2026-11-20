#!/usr/bin/env python3
"""
Debian Package (.deb) Generator for SolarAd on Raspberry Pi (Linux ARM64).
Creates a 100% compliant Debian package directly without requiring dpkg-deb.
"""

import os
import sys
import shutil
import tarfile
import struct
import argparse
import tempfile

def create_ar_archive(ar_path, file_entries):
    """
    Creates a standard 'ar' archive containing the given files in order.
    file_entries: list of (filename_in_ar, path_to_real_file)
    """
    with open(ar_path, "wb") as ar:
        ar.write(b"!<arch>\n")
        for name, file_path in file_entries:
            size = os.path.getsize(file_path)
            # ar header format:
            # 16 bytes filename (padded with spaces, terminated with /)
            # 12 bytes timestamp
            # 6 bytes owner ID
            # 6 bytes group ID
            # 8 bytes file mode (octal)
            # 10 bytes file size
            # 2 bytes end magic (b"`\n")
            header_name = name.ljust(16)
            timestamp = "0".ljust(12)
            uid = "0".ljust(6)
            gid = "0".ljust(6)
            mode = "100644".ljust(8)
            size_str = str(size).ljust(10)
            magic = "`\n"
            
            header = f"{header_name}{timestamp}{uid}{gid}{mode}{size_str}{magic}".encode("ascii")
            ar.write(header)
            
            with open(file_path, "rb") as f:
                shutil.copyfileobj(f, ar)
            
            # 2-byte alignment padding if size is odd
            if size % 2 != 0:
                ar.write(b"\n")

def make_tar_gz(source_dir, output_tar_gz):
    """Creates a tar.gz where all files are root-owned using GNU tar format (required by dpkg)."""
    with tarfile.open(output_tar_gz, "w:gz", format=tarfile.GNU_FORMAT) as tar:
        for root, dirs, files in os.walk(source_dir):
            for d in sorted(dirs):
                full_path = os.path.join(root, d)
                rel_path = os.path.relpath(full_path, source_dir)
                info = tar.gettarinfo(full_path, arcname="./" + rel_path)
                info.uid = 0
                info.gid = 0
                info.uname = "root"
                info.gname = "root"
                tar.addfile(info)
            for f in sorted(files):
                full_path = os.path.join(root, f)
                rel_path = os.path.relpath(full_path, source_dir)
                info = tar.gettarinfo(full_path, arcname="./" + rel_path)
                info.uid = 0
                info.gid = 0
                info.uname = "root"
                info.gname = "root"
                with open(full_path, "rb") as file_obj:
                    tar.addfile(info, file_obj)

def main():
    parser = argparse.ArgumentParser(description="Package AdDash into Debian .deb for Raspberry Pi")
    parser.add_argument("--lib-dir", required=True, help="Directory containing all dependency JARs")
    parser.add_argument("--launcher", required=True, help="Path to launcher shell script")
    parser.add_argument("--desktop", required=True, help="Path to .desktop entry")
    parser.add_argument("--control", required=True, help="Path to Debian control file")
    parser.add_argument("--icon", required=True, help="Path to 512x512 PNG app icon")
    parser.add_argument("--output", required=True, help="Output .deb file path")
    args = parser.parse_args()

    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)

    with tempfile.TemporaryDirectory() as temp_dir:
        # 1. Create debian-binary
        debian_binary_path = os.path.join(temp_dir, "debian-binary")
        with open(debian_binary_path, "w", newline="") as f:
            f.write("2.0\n")

        # 2. Build control directory
        control_dir = os.path.join(temp_dir, "control_dir")
        os.makedirs(control_dir, exist_ok=True)
        shutil.copy2(args.control, os.path.join(control_dir, "control"))
        
        control_tar_gz = os.path.join(temp_dir, "control.tar.gz")
        make_tar_gz(control_dir, control_tar_gz)

        # 3. Build data directory (root filesystem structure)
        data_dir = os.path.join(temp_dir, "data_dir")
        opt_addash = os.path.join(data_dir, "opt", "addash")
        opt_bin = os.path.join(opt_addash, "bin")
        opt_lib = os.path.join(opt_addash, "lib")
        usr_bin = os.path.join(data_dir, "usr", "bin")
        usr_apps = os.path.join(data_dir, "usr", "share", "applications")
        usr_icons = os.path.join(data_dir, "usr", "share", "icons", "hicolor", "512x512", "apps")
        usr_pixmaps = os.path.join(data_dir, "usr", "share", "pixmaps")

        for d in [opt_bin, opt_lib, usr_bin, usr_apps, usr_icons, usr_pixmaps]:
            os.makedirs(d, exist_ok=True)

        # Copy JARs
        for fname in os.listdir(args.lib_dir):
            if fname.endswith(".jar"):
                shutil.copy2(os.path.join(args.lib_dir, fname), os.path.join(opt_lib, fname))

        # Copy launcher script
        dest_launcher = os.path.join(opt_bin, "addash")
        shutil.copy2(args.launcher, dest_launcher)
        os.chmod(dest_launcher, 0o755)

        # Create shell wrapper in /usr/bin/addash
        usr_bin_addash = os.path.join(usr_bin, "addash")
        with open(usr_bin_addash, "w") as f:
            f.write("#!/bin/sh\nexec /opt/addash/bin/addash \"$@\"\n")
        os.chmod(usr_bin_addash, 0o755)

        # Copy .desktop entry
        shutil.copy2(args.desktop, os.path.join(usr_apps, "addash.desktop"))

        # Copy icons
        shutil.copy2(args.icon, os.path.join(usr_icons, "addash.png"))
        shutil.copy2(args.icon, os.path.join(usr_pixmaps, "addash.png"))

        data_tar_gz = os.path.join(temp_dir, "data.tar.gz")
        make_tar_gz(data_dir, data_tar_gz)

        # 4. Pack into .deb using ar format
        create_ar_archive(args.output, [
            ("debian-binary", debian_binary_path),
            ("control.tar.gz", control_tar_gz),
            ("data.tar.gz", data_tar_gz)
        ])

    print(f"Successfully generated Debian package: {args.output}")

if __name__ == "__main__":
    main()
