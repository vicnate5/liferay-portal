#!/bin/bash

REPOSITORY_NAME="$1"
GITID_TEST="$2"
BRANCH_NAME_TEST="$3"
TEST_FILE="$4"
UPGRADE_FLAG="$5"

BUNDLES_PATH_1="$HOME/dev/projects/bundles"
BUNDLES_PATH_2="$HOME/dev/bundles/master"

if [ -n "$TEST_FILE" ]; then
    # ANSI escape sequences
    RED_BOLD='\033[1;31m'
    GREEN_BOLD='\033[1;32m'
    RESET='\033[0m'

    echo -e "${RED_BOLD}Attention: The test will be executed with the previously configured database!${RESET}
    
    Perhaps you are interested in resetting it or configuring something different according to what you are trying to reproduce."

    # Confirmation question
    read -p "Do you want to proceed with the test? (Y/N): " confirm
    if [[ "$confirm" == [yY] || "$confirm" == [yY][sS] ]]; then
        echo -e "${GREEN_BOLD}Running.${RESET}"
    else
        echo "Operation canceled by the user."
        exit 0
    fi
fi

# Check if the bundles folder exists
if [ -d "$BUNDLES_PATH_1" ]; then
    BUNDLES_PATH="$BUNDLES_PATH_1"
elif [ -d "$BUNDLES_PATH_2" ]; then
    BUNDLES_PATH="$BUNDLES_PATH_2"
else
    echo "The bundles folder was not found in any of the expected locations."
    exit 1
fi

echo "Cleaning up the bundles folder in $BUNDLES_PATH."

# Delete logs folder
rm -rf "$BUNDLES_PATH"/logs

# Delete data folder
rm -rf "$BUNDLES_PATH"/data

# Delete state folder
rm -rf "$BUNDLES_PATH"/osgi/state

# Delete temp folder 
if [ -d "$BUNDLES_PATH/tomcat-9.0.75" ]; then
    rm -rf "$BUNDLES_PATH"/tomcat-9.0.75/temp

    rm -rf "$BUNDLES_PATH"/tomcat-9.0.75/work
else
    echo "tomcat-9.0.75 not found. Maybe tomcat has been bumped."
fi

echo "Bundles folder cleared successfully."

git fetch https://github.com/"$REPOSITORY_NAME"/liferay-portal/ pull/"$GITID_TEST"/head:"$BRANCH_NAME_TEST"
git checkout "$BRANCH_NAME_TEST"
git rebase master
ant clean
ant all

if [ -n "$TEST_FILE" ]; then
    echo "Executing : $TEST_FILE"
    if [ "$UPGRADE_FLAG" == "-u" ]; then
        ant -f build-test-tomcat.xml run-selenium-tomcat -Dportal.legacy.dir="/home/me/dev/projects/liferay-qa-portal-legacy-ee" -Dtest.class="$TEST_FILE"
    else
        ant -f build-test-tomcat.xml run-selenium-tomcat -Dtest.class="$TEST_FILE"
    fi
fi