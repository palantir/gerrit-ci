#!/bin/bash
set -x
set -e
date

# Workaround bozos marking our directories unwritable by us (which would make
# cleaning up our own files with git clean fail and error out)
chmod -R u+w .

# Clean out cruft files and any modifications made by previous builds
git clean -fdx --quiet
git reset --hard --quiet

# Nuke/refetch tags; behave like a clean clone.  For people that do BAD things
#git for-each-ref --format='delete %(refname)' refs/tags | git update-ref --stdin
#git fetch --tags --quiet

# Get out of temporary states
git merge --abort       2>/dev/null && echo "Aborted previous in-progress merge"
git rebase --abort      2>/dev/null && echo "Aborted previous in-progress rebase"
git am --abort          2>/dev/null && echo "Aborted previous in-progress am"
git revert --abort      2>/dev/null && echo "Aborted previous in-progress revert"
git cherry-pick --abort 2>/dev/null && echo "Aborted previous in-progress cherry-pick"

# The above aborts may have rewound us to a different commit.  Return to the
# testing commit.
git checkout --quiet $GIT_COMMIT

### END PREBUILD-COMMANDS ###
