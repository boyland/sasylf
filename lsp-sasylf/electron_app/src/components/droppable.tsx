import React from "react";
import { useDroppable } from "@dnd-kit/core";

export default function Droppable(props: any) {
	const { isOver, setNodeRef } = useDroppable({
		id: props.id,
	});
	const style = {
		opacity: isOver ? 0.5 : 1,
	};

	return (
		<div className={`${props.className} p-2`} ref={setNodeRef} style={style}>
			{props.children}
		</div>
	);
}
