#!/bin/bash
for branch in $(git for-each-ref --format='%(refname:short)' refs/heads/); do
  root=$(git rev-list --max-parents=0 "$branch" 2>/dev/null | head -1)
  root_msg=$(git log --oneline -1 "$root" 2>/dev/null)
  echo "$branch -> $root_msg"
done
