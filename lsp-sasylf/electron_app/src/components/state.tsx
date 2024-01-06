import { createContext } from "react";
import { UniqueIdentifier } from "@dnd-kit/core";

type Context = {
	dropped: any;
	removeHandler: (id: number) => void;
	addHandler: (id: UniqueIdentifier, text: string) => void;
};

export const DroppedContext = createContext<Context>({
	dropped: {},
	removeHandler: (_) => {},
	addHandler: (_, __) => {},
});
