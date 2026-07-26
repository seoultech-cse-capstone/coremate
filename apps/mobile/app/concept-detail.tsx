import { router, useLocalSearchParams } from "expo-router";

import { ConceptDetail } from "../src/features/content/components/ConceptDetail";

export default function ConceptDetailScreen() {
  const { conceptId } = useLocalSearchParams<{ conceptId?: string }>();
  const parsedConceptId = Number(conceptId);
  const resolvedConceptId = Number.isFinite(parsedConceptId) ? parsedConceptId : null;

  return (
    <ConceptDetail conceptId={resolvedConceptId} onBack={() => router.back()} />
  );
}
