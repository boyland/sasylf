import { createContext, RefObject } from "react";
import { ast } from "../types";

type Context = {
	addRef: (id: number, ref: RefObject<HTMLDivElement>) => void;
	ast: ast | null;
};

export const DroppedContext = createContext<Context>({
	addRef: (_, __) => {},
	ast: null,
});

export const FileContext = createContext("");
