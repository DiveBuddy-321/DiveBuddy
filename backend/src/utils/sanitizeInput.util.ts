export const sanitizeArgs = (args: unknown[]): unknown[] => {
  return args.map(arg => {
    // Don't sanitize Error objects - they naturally contain newlines in stack traces
    if (arg instanceof Error) {
      return arg;
    }
    // For other types, convert to string and sanitize
    return sanitizeInput(String(arg));
  });
};

export const sanitizeInput = (input: string): string => {
  if (/[\r\n]/.test(input)) {
    throw new Error('CRLF injection attempt detected');
  }
  return input;
};
