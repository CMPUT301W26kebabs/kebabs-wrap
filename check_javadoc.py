import os
import re

src_dir = os.path.join('app', 'src', 'main', 'java', 'com', 'example', 'eventmanager')

files_missing_javadoc = []

for root, _, files in os.walk(src_dir):
    for f in files:
        if f.endswith('.java'):
            file_path = os.path.join(root, f)
            with open(file_path, 'r', encoding='utf-8') as file:
                content = file.read()
                
            # Naive match for methods: modifiers? return_type name(...) {
            # This regex looks for typical method signatures.
            method_pattern = re.compile(r'(?:(?:public|private|protected|static|final|\s)\s+)*[\w\<\>\[\]]+\s+(\w+)\s*\([^\)]*\)\s*(?:throws\s+[\w\s,]+)?\{')
            methods = list(method_pattern.finditer(content))
            
            missing_count = 0
            for m in methods:
                method_name = m.group(1)
                if method_name in ['if', 'for', 'while', 'switch', 'catch', 'synchronized']:
                    continue
                start_pos = m.start()
                # Check preceding text for Javadoc
                preceding_text = content[:start_pos].strip()
                if not preceding_text.endswith('*/'):
                    missing_count += 1
            
            if missing_count > 0:
                files_missing_javadoc.append((f, missing_count))

for f, count in files_missing_javadoc:
    print(f"{f}: {count} missing javadocs")
