import { sanitizeArgs, sanitizeInput } from './sanitizeInput.util';

const logger = {
  info: (message: string, ...args: unknown[]) => {
    console.debug(`[INFO] ${sanitizeInput(message)}`, ...sanitizeArgs(args));
  },
  error: (message: string, ...args: unknown[]) => {
    console.error(`[ERROR] ${sanitizeInput(message)}`, ...sanitizeArgs(args));
  },
};

export default logger;
