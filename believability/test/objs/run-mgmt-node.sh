#!/bin/sh

. ./common.sh

out_name=mgmt-run-`timestamp`.out

./run-node.sh ManagementAgentNode 2>&1 | tee $out_name

echo ""
echo ""
echo ""
echo "Output saved to: $out_name"

