with open('core/src/test/java/com/vibeplay/core/engine/VibeCookerEngineTest.kt', 'r') as file:
    data = file.read()

import re

# Remove the conflicted section at the end and rely on the new ones from origin/main
# which handle empty track list and danceability correctly. The only thing missing from origin/main
# but present in our PR was already added in origin/main as `scoreAndRankTracks applies harmonic bonus for target key in rules`.

# Let's write the correct file content.

file_path = 'core/src/test/java/com/vibeplay/core/engine/VibeCookerEngineTest.kt'
with open(file_path, 'r') as f:
    content = f.read()

# Our PR added the tests that overlap with the ones in `main`. Let's just use the `main` ones
# since they are better named and one of them checks the exact diff score difference.

conflict_pattern = r'<<<<<<< HEAD.*?=======(.*?)>>>>>>> origin/main'
resolved_content = re.sub(conflict_pattern, r'\1', content, flags=re.DOTALL)

with open(file_path, 'w') as f:
    f.write(resolved_content)
