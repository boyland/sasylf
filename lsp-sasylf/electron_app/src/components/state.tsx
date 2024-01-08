import { createContext, RefObject } from "react";
import { UniqueIdentifier } from "@dnd-kit/core";

type Context = {
	dropped: any;
	addRef: (id: number, ref: RefObject<HTMLDivElement>) => void;
	removeHandler: (id: number) => void;
	addHandler: (id: UniqueIdentifier, text: string) => void;
};

export const DroppedContext = createContext<Context>({
	dropped: {},
	addRef: (_, __) => {},
	removeHandler: (_) => {},
	addHandler: (_, __) => {},
});
