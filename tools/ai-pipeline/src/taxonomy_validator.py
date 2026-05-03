from collections import defaultdict


def collect_concepts(taxonomy: dict) -> list[dict]:
    concepts = []

    for lv1 in taxonomy["category_lv1_list"]:
        for lv2 in lv1["category_lv2_list"]:
            for concept in lv2["concept_candidates"]:
                concepts.append({
                    "category_lv1_name": lv1["category_lv1_name"],
                    "category_lv2_name": lv2["category_lv2_name"],
                    **concept,
                })

    return concepts


def validate_taxonomy(taxonomy: dict, exclude_topics: list[str] | None = None) -> list[str]:
    warnings = []
    concepts = collect_concepts(taxonomy)

    name_map = defaultdict(list)

    for concept in concepts:
        name_map[concept["concept_name"]].append(concept)

    for concept_name, items in name_map.items():
        if len(items) > 1:
            locations = [
                f"{item['category_lv1_name']} > {item['category_lv2_name']}"
                for item in items
            ]
            warnings.append(
                f"[중복 concept_name] {concept_name}: {locations}"
            )

    if exclude_topics:
        for concept in concepts:
            for keyword in exclude_topics:
                if keyword in concept["concept_name"]:
                    warnings.append(
                        f"[제외 대상 포함] {concept['concept_name']} "
                        f"위치: {concept['category_lv1_name']} > {concept['category_lv2_name']}"
                    )

    return warnings