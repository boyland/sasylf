import { createContext, RefObject, MouseEventHandler } from "react";
import { ast } from "../types";

type Context = {
	addRef: (id: number, ref: RefObject<HTMLDivElement>) => void;
	showContextMenu: (event: React.MouseEvent, nodeId: number) => void;
	ast: ast | null;
};

export const NodeContext = createContext<Context>({
	addRef: (_, __) => {},
	showContextMenu: (_, __) => {},
	ast: null,
});

export const FileContext = createContext("");
