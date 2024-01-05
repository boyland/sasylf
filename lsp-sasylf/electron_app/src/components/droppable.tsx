import React from "react";
import { useDroppable } from "@dnd-kit/core";

interface droppableProps {
	id: number;
	children: any;
	className: string;
}

export default function Droppable(props: droppableProps) {
	const { isOver, setNodeRef } = useDroppable({
		id: props.id,
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
