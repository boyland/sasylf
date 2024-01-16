import { createContext, RefObject } from "react";
import { UniqueIdentifier } from "@dnd-kit/core";
import { ast } from "../types";

type Context = {
	dropped: any;
	addRef: (id: number, ref: RefObject<HTMLDivElement>) => void;
	removeHandler: (id: number) => void;
	addHandler: (id: UniqueIdentifier, text: string) => void;
	ast: ast | null;
};

export const DroppedContext = createContext<Context>({
	dropped: {},
	addRef: (_, __) => {},
	removeHandler: (_) => {},
	addHandler: (_, __) => {},
	ast: null,
});

export const FileContext = createContext("");
