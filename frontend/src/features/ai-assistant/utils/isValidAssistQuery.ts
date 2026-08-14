export const MIN_FREE_TEXT_LENGTH = 10;
export const MIN_FAULT_CODE_LENGTH = 3;
const MIN_DISTINCT_CHARACTERS = 3;
const MAX_SINGLE_CHAR_RATIO = 0.6;

const FAULT_CODE_PATTERN =
  /\b(?:2310-TRV|A581-TRV|OUt[1-9]|OC\d?|OV\d?|PV\d+|E\d{2}|A\.\d+|[A-Z]{1,2}\d{3,4}[A-Z0-9]?|F\d{3}|\d{4})\b/i;

const SEMANTIC_PATTERN =
  /\b(?:pompe|moteur|variateur|filature|convoyeur|convoyage|hitachi|abb|goodrive|veichi|siemens|acs880|sj200|si23|station|solaire|photovolta)/i;

export function containsFaultCode(query: string): boolean {
  return FAULT_CODE_PATTERN.test(query.trim());
}

export function isValidAssistQuery(content: string): boolean {
  const trimmed = content.trim();
  if (!trimmed) {
    return false;
  }
  if (containsFaultCode(trimmed) && trimmed.length >= MIN_FAULT_CODE_LENGTH) {
    return true;
  }
  if (SEMANTIC_PATTERN.test(trimmed) && trimmed.length >= MIN_FAULT_CODE_LENGTH) {
    return true;
  }
  if (trimmed.length < MIN_FREE_TEXT_LENGTH) {
    return false;
  }
  return hasMeaningfulFreeText(trimmed);
}

function hasMeaningfulFreeText(trimmed: string): boolean {
  if (hasDominantRepeatedCharacter(trimmed)) {
    return false;
  }
  const distinctChars = new Set(trimmed.toLowerCase()).size;
  if (distinctChars < MIN_DISTINCT_CHARACTERS) {
    return false;
  }
  const significantWords = trimmed
    .split(/\s+/)
    .map((word) => word.replace(/[^\p{L}]/gu, ''))
    .filter((word) => word.length >= 2);
  return significantWords.length >= 2;
}

function hasDominantRepeatedCharacter(trimmed: string): boolean {
  const counts = new Map<string, number>();
  for (const char of trimmed) {
    counts.set(char, (counts.get(char) ?? 0) + 1);
  }
  for (const count of counts.values()) {
    if (count / trimmed.length > MAX_SINGLE_CHAR_RATIO) {
      return true;
    }
  }
  return false;
}
