#!/bin/zsh
set -e
cd "$(dirname "$0")/.."
python3 Scripts/owner-signing-session.py
printf '\nSigning session ended. Return to Codex for final verification; nothing was published.\n'
read -r '?Press Return to close this window.'
