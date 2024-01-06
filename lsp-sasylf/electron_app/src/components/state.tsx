import { createContext } from "react";

type Context = {
	dropped: any;
	removeHandler: (_: number) => void;
};

export const DroppedContext = createContext<Context>({
	dropped: {},
	removeHandler: (_: number) => {},
});
