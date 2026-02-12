#!/bin/bash

echo "========================================"
echo "  Eureka Service Instance Status Check  "
echo "========================================"
echo ""

# Check if Eureka is accessible
if ! curl -s http://localhost:8761/actuator/health > /dev/null 2>&1; then
    echo "❌ Eureka Server is not accessible at http://localhost:8761"
    exit 1
fi

echo "✅ Eureka Server is running at http://localhost:8761"
echo ""
echo "Registered Service Instances:"
echo "-----------------------------"

# Services to check
services=("ACCOUNT" "DEPOSIT" "LOAN" "CARD")

for service in "${services[@]}"; do
    # Get instance count
    count=$(curl -s http://localhost:8761/eureka/apps/$service 2>/dev/null | grep -c instanceId || echo 0)

    if [ $count -eq 0 ]; then
        echo "❌ $service: No instances registered"
    elif [ $count -eq 3 ]; then
        echo "✅ $service: $count instances (properly scaled)"
    else
        echo "⚠️  $service: $count instances (expected 3)"
    fi

    # Show instance IDs if any are registered
    if [ $count -gt 0 ]; then
        echo "   Instance IDs:"
        curl -s http://localhost:8761/eureka/apps/$service 2>/dev/null | \
            grep instanceId | \
            sed 's/.*<instanceId>\(.*\)<\/instanceId>.*/      - \1/'
    fi
done

echo ""
echo "API Gateway Status:"
echo "-------------------"
gateway_count=$(curl -s http://localhost:8761/eureka/apps/API-GATEWAY 2>/dev/null | grep -c instanceId || echo 0)
if [ $gateway_count -gt 0 ]; then
    echo "✅ API-GATEWAY: $gateway_count instance(s) registered"
else
    echo "❌ API-GATEWAY: Not registered"
fi

echo ""
echo "========================================"
echo "Dashboard URL: http://localhost:8761"
echo "========================================"