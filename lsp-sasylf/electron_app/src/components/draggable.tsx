import React from "react";
import { useDraggable } from "@dnd-kit/core";

export default function Draggable(props: any) {
	const Element = props.element || "div";
	const { attributes, listeners, setNodeRef } = useDraggable({
		id: props.id,
	});

	return (
		<Element ref={setNodeRef} {...listeners} {...attributes}>
			{props.children}
		</Element>
	);
}
