#!/bin/bash
cd /home/kavia/workspace/code-generation/cgpa-calculator-mobile-app-161743-161752/cgpa_calculator_mobile_app
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

