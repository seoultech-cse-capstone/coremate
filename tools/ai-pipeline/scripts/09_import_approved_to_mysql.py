import _bootstrap

import argparse
import json

from server.models.content_import import ContentImportRequest
from server.services.content_import_service import import_approved_content


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Import approved CoreMate AI pipeline JSON artifacts into MySQL.",
    )
    parser.add_argument("subject_code", help="Subject code matching configs/{SUBJECT_CODE}.yaml")
    parser.add_argument(
        "--execute",
        action="store_true",
        help="Write to MySQL. Without this flag, the script only validates the import plan.",
    )
    parser.add_argument(
        "--activate",
        action="store_true",
        help="Mark imported review-controlled content as active.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    response = import_approved_content(
        subject_code=args.subject_code,
        request=ContentImportRequest(
            dry_run=not args.execute,
            activate_on_import=args.activate,
        ),
    )
    payload = (
        response.model_dump(mode="json")
        if hasattr(response, "model_dump")
        else response.dict()
    )
    print(json.dumps(payload, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
