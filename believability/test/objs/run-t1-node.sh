#!/bin/sh

. ./common.sh

out_name=t1-run-`timestamp`.out

./run-node.sh T1  2>&1 | tee $out_name

echo ""
echo ""
echo ""
echo "Output saved to: $out_name"

