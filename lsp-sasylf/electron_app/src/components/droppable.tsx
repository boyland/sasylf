import React from "react";
import { useDroppable } from "@dnd-kit/core";
import { Data } from "../types";

interface droppableProps {
	id: number;
	children: any;
	className: string;
	data?: Data;
}

export default function Droppable(props: droppableProps) {
	const { isOver, setNodeRef } = useDroppable({
		id: props.id,
		data: props.data,
	});
	const style = {
		opacity: isOver ? 0.5 : 1,
		color: isOver ? "green" : "black",
	};

	return (
		<div className={`${props.className} p-2`} ref={setNodeRef} style={style}>
			{props.children}
		</div>
	);
}
