# Image/Paste/URL sharing API

## Starting up the API

1. Replace the values in `env.sh` with your own values
2. Run `sh launcher.sh` to start the API 

## Image uploading automation

<details>
<summary>Linux - (XFCE4 DE only)</summary>

1. Copy `vars.sh`, `common.sh` and `upload-image.sh` scripts from `scripts` directory into `/usr/lib/xfce4/screenshooter/scripts/`
2. Open `vars.sh` and set the `BASE_URL` variable to the API server URL and `API_KEY` to the API key (you have to create an user account manually in the DB)
3. Open `xfce4-screenshooter` and go to `Preferences` tab, create new custom action and set the command to `/usr/lib/xfce4/screenshooter/scripts/upload-image.sh %f`
4. After capturing a screenshot, select the custom action from the list and the image will be uploaded to the API server
</details>

<details>
<summary>Windows - ShareX (TODO)</summary>
TODO 
</details>


