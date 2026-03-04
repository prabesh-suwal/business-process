#!/bin/bash

SESSION=oauth
BASE=/home/prabesh/1work/mvp/oauth

# ──────────────────────────────────────────────
# JVM Heap Tuning for Dev Mode
# Heavy services: 256 MB  |  Light services: 128 MB
# Total worst-case: ~2 GB for all services
# ──────────────────────────────────────────────
declare -A heap_sizes=(
  ["cas-server"]="256m"
  ["workflow-service"]="256m"
  ["memo-service"]="192m"
  ["api-gateway"]="192m"
  ["wfm-gateway"]="192m"
  ["policy-engine-service"]="128m"
  ["organization-service"]="128m"
  ["form-service"]="128m"
  ["document-service"]="128m"
  ["notification-service"]="128m"
  ["person-service"]="96m"
  ["audit-service"]="96m"
  ["swagger-docs-service"]="96m"
)

services=(
  "api-gateway"
  "cas-server"
  # "form-service"
  "organization-service"
  "policy-engine-service"
  # "workflow-service"
)

colors=(
  "red"
  "green"
  "yellow"
  "blue"
  "magenta"
  "cyan"
  "white"
  "brightred"
  "brightblue"
)

tmux has-session -t $SESSION 2>/dev/null && tmux kill-session -t $SESSION

tmux new-session -d -s $SESSION

for i in "${!services[@]}"; do
  if [ "$i" -ne 0 ]; then
    tmux split-window -t $SESSION -v
    tmux select-layout -t $SESSION tiled
  fi

  svc="${services[$i]}"
  heap="${heap_sizes[$svc]:-128m}"
  jvm_opts="-Xms64m -Xmx${heap} -XX:+UseZGC -XX:+ZGenerational"

  tmux select-pane -t $SESSION:0.$i
  tmux select-pane -T "#[fg=${colors[$i]}] ${svc}"
  tmux send-keys "cd $BASE/${svc} && JAVA_TOOL_OPTIONS='${jvm_opts}' mvn spring-boot:run" C-m
done

tmux attach -t $SESSION
