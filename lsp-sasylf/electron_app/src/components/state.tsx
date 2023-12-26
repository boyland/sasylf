import { createContext } from "react";

export const DroppedContext = createContext<[any, (id: number) => void]>([
	{},
	(_: number) => {},
]);
