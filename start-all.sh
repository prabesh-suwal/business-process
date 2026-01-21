#!/bin/bash

SESSION=oauth
BASE=/home/prabesh/1work/mvp/oauth

services=(
  "admin-gateway"
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

  tmux select-pane -t $SESSION:0.$i
  tmux select-pane -T "#[fg=${colors[$i]}] ${services[$i]}"
  tmux send-keys "cd $BASE/${services[$i]} && mvn spring-boot:run" C-m
done

tmux attach -t $SESSION
