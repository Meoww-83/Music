#!/data/data/com.termux/files/usr/bin/bash
# CI poll: every 10s until run completes, then dump conclusion (+ failed logs on failure).
set -u
RUN="${1:-38719477400}"
TOK=$(grep -oE 'ghp_[A-Za-z0-9]+' "$HOME/build.txt" | head -1)
export GH_TOKEN="$TOK"
for i in $(seq 1 270); do
  out=$(gh run view "$RUN" -R Meoww-83/Music --json status,conclusion 2>/dev/null) || out=""
  st=$(echo "$out" | sed -n 's/.*"status":"\([a-z_]*\)".*/\1/p')
  cc=$(echo "$out" | sed -n 's/.*"conclusion":"\([a-z_]*\)".*/\1/p')
  [ $((i % 6)) -eq 1 ] && echo "$(date +%T) poll#$i status=${st:-?} conclusion=${cc:-?}"
  if [ "$st" = "completed" ]; then
    echo "=== RUN $RUN FINISHED: $cc ==="
    if [ "$cc" = "success" ]; then
      gh run view "$RUN" -R Meoww-83/Music
      echo "BUILD GREEN"
    else
      gh run view "$RUN" -R Meoww-83/Music
      echo "=== FAILED STEP LOGS (first 600 lines) ==="
      gh run view "$RUN" -R Meoww-83/Music --log-failed 2>/dev/null | head -600
    fi
    exit 0
  fi
  sleep 10
done
echo "=== TIMEOUT after 45 min of polling ==="
gh run view "$RUN" -R Meoww-83/Music || true
exit 1
