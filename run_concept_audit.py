import sys
import os
import re
from collections import OrderedDict

# Add ai directory to path
sys.path.append(os.path.join(os.path.dirname(__file__), 'ai'))
import domain_banks

banks = domain_banks.DOMAIN_KNOWLEDGE_BANKS

subject_map = [
    ('Software Testing', 'software testing'),
    ('Database Management Systems', 'database management systems'),
    ('Operating Systems', 'operating systems'),
    ('Computer Networks', 'computer networks'),
    ('Data Structures & Algorithms', 'data structures'),
    ('Machine Learning', 'machine learning'),
    ('Blockchain Development', 'blockchain development'),
    ('Cloud Security', 'cloud security'),
    ('Digital Marketing', 'digital marketing'),
    ('Quantum Computing', 'quantum computing'),
    ('Artificial Intelligence', 'artificial intelligence'),
    ('Discrete Mathematics', 'discrete mathematical structures')
]

def normalize_concept_name(raw):
    if not raw or not raw.strip():
        return 'General Concept'
    c = raw.strip()
    c = re.sub(r'(?i)^(Regarding fundamental principles of|In an operational engineering context for|Under high-scale production constraints evaluating|In foundational study of|When implementing practical workflows for|Advanced application of|Foundational principles of)\s+', '', c)
    c = re.sub(r'(?i)\s+(Implementation|Architecture|Foundations|Concepts|Mechanics|Principles)?\s*[-|\[\(]?\s*(EASY|MEDIUM|HARD|Tier\s*\d+)?\s*#?\d+[\]\)]?', '', c)
    c = re.sub(r'(?i)\s*[-|\[\(]?\s*(EASY|MEDIUM|HARD|Tier\s*\d+)\s*[\]\)]?', '', c)
    c = re.sub(r'(?i)\s*#\d+$', '', c)
    if re.search(r'(?i).+\s+(Implementation|Architecture|Foundations|Mechanics|Concepts|Principles)$', c) and c.lower() not in ['software architecture', 'system architecture']:
        c = re.sub(r'(?i)\s+(Implementation|Architecture|Foundations|Mechanics|Concepts|Principles)$', '', c)
    c = re.sub(r'^[:\s-]+|[:\s-]+$', '', c)
    return c.strip()

report_lines = []
report_lines.append("# COMPLETE CONCEPT COVERAGE AUDIT REPORT")
report_lines.append("")
report_lines.append("**Single Source of Truth**: `ai/domain_banks.py`")
report_lines.append("**Audit Scope**: All 12 Frontend Subjects & Canonical Question Banks")
report_lines.append("")

master_table_rows = []
reconciliation_summary = []

for frontend_subj, bank_key in subject_map:
    bank = banks.get(bank_key, {})
    easy_list = bank.get('EASY', [])
    medium_list = bank.get('MEDIUM', [])
    hard_list = bank.get('HARD', [])
    
    total_q = len(easy_list) + len(medium_list) + len(hard_list)
    
    # Raw concept counts and difficulty breakdown
    raw_concepts = OrderedDict()
    
    for diff, q_list in [('EASY', easy_list), ('MEDIUM', medium_list), ('HARD', hard_list)]:
        for q in q_list:
            raw_c = q.get('concept', 'Unknown')
            if raw_c not in raw_concepts:
                raw_concepts[raw_c] = {'EASY': 0, 'MEDIUM': 0, 'HARD': 0, 'TOTAL': 0}
            raw_concepts[raw_c][diff] += 1
            raw_concepts[raw_c]['TOTAL'] += 1

    # Normalized grouping
    norm_groups = OrderedDict()
    for raw_c, counts in raw_concepts.items():
        norm_c = normalize_concept_name(raw_c)
        if norm_c not in norm_groups:
            norm_groups[norm_c] = {'variants': OrderedDict(), 'EASY': 0, 'MEDIUM': 0, 'HARD': 0, 'TOTAL': 0}
        norm_groups[norm_c]['variants'][raw_c] = counts
        norm_groups[norm_c]['EASY'] += counts['EASY']
        norm_groups[norm_c]['MEDIUM'] += counts['MEDIUM']
        norm_groups[norm_c]['HARD'] += counts['HARD']
        norm_groups[norm_c]['TOTAL'] += counts['TOTAL']
        
        master_table_rows.append((frontend_subj, norm_c, raw_c, counts['TOTAL'], counts['EASY'], counts['MEDIUM'], counts['HARD']))

    # Reconciliation Check
    raw_tot_check = sum(c['TOTAL'] for c in raw_concepts.values())
    norm_tot_check = sum(g['TOTAL'] for g in norm_groups.values())
    reconciled = (total_q == raw_tot_check == norm_tot_check)
    reconciliation_summary.append({
        'subject': frontend_subj,
        'bank_key': bank_key,
        'total': total_q,
        'raw_sum': raw_tot_check,
        'norm_sum': norm_tot_check,
        'reconciled': reconciled,
        'unique_raw': len(raw_concepts),
        'unique_norm': len(norm_groups)
    })

    report_lines.append("==================================================")
    report_lines.append(f"SUBJECT: {frontend_subj}")
    report_lines.append(f"BANK KEY: {bank_key}")
    report_lines.append(f"TOTAL: {total_q}")
    report_lines.append(f"EASY: {len(easy_list)}")
    report_lines.append(f"MEDIUM: {len(medium_list)}")
    report_lines.append(f"HARD: {len(hard_list)}")
    report_lines.append("")
    
    report_lines.append("CONCEPT COVERAGE (RAW QUESTION BANK CONCEPTS):")
    for idx, (raw_c, counts) in enumerate(raw_concepts.items(), 1):
        tot = counts['TOTAL']
        e = counts['EASY']
        m = counts['MEDIUM']
        h = counts['HARD']
        report_lines.append(f"  {idx:2d}. {raw_c:60s} | Total: {tot:2d} | Easy: {e:2d} | Medium: {m:2d} | Hard: {h:2d}")
    report_lines.append("")
    
    report_lines.append("NORMALIZED CONCEPT GROUPS:")
    for norm_c, gdata in norm_groups.items():
        tot = gdata['TOTAL']
        e = gdata['EASY']
        m = gdata['MEDIUM']
        h = gdata['HARD']
        report_lines.append(f"- **{norm_c}** (Total: {tot} questions | Easy: {e}, Medium: {m}, Hard: {h})")
        for var_c, vcounts in gdata['variants'].items():
            vtot = vcounts['TOTAL']
            ve = vcounts['EASY']
            vm = vcounts['MEDIUM']
            vh = vcounts['HARD']
            report_lines.append(f"  - `{var_c}` (Total: {vtot}, Easy: {ve}, Med: {vm}, Hard: {vh})")
    report_lines.append("")

report_lines.append("==================================================")
report_lines.append("MASTER CONCEPT COVERAGE TABLE")
report_lines.append("==================================================")
report_lines.append("")
report_lines.append("| Subject | Normalized Concept | Raw Concept Variant | Question Count | Easy | Medium | Hard |")
report_lines.append("| :--- | :--- | :--- | :---: | :---: | :---: | :---: |")

for subj, norm_c, raw_c, tot, e, m, h in master_table_rows:
    report_lines.append(f"| {subj} | {norm_c} | `{raw_c}` | {tot} | {e} | {m} | {h} |")

report_lines.append("")
report_lines.append("==================================================")
report_lines.append("RECONCILIATION SUMMARY")
report_lines.append("==================================================")
report_lines.append("")
report_lines.append("| Subject | Bank Key | Total Questions | Raw Concept Count | Normalized Concept Count | Reconciled |")
report_lines.append("| :--- | :--- | :---: | :---: | :---: | :---: |")

for r in reconciliation_summary:
    rec_status = "PASS" if r['reconciled'] else "FAIL"
    report_lines.append(f"| {r['subject']} | `{r['bank_key']}` | {r['total']} | {r['unique_raw']} | {r['unique_norm']} | **{rec_status}** |")

output_text = "\n".join(report_lines)
target_path = os.path.join(os.path.dirname(__file__), 'concept_coverage_audit.md')
with open(target_path, 'w', encoding='utf-8') as f:
    f.write(output_text)

print("Audit report successfully generated and saved to concept_coverage_audit.md!")
print("Reconciliation results:")
for r in reconciliation_summary:
    print(f"  {r['subject']:30s} | Total: {r['total']:3d} | Reconciled: {r['reconciled']}")
