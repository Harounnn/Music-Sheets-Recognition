import subprocess

def run_oemer(input_image, output_dir, use_tf=False, save_cache=True, deskew=True):
    cmd = ["oemer", input_image, "--output-path", output_dir]
    if use_tf:
        cmd.append("--use-tf")
    if save_cache:
        cmd.append("--save-cache")
    if not deskew:
        cmd.append("--without-deskew")
    subprocess.check_call(cmd)
    print("Done")
