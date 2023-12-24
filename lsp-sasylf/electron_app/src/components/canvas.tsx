import React, { useRef, useState } from "react";

const Canvas = () => {
	const canvasRef = useRef(null);
	const [scale, setScale] = useState(1);
	const [pan, setPan] = useState({ x: 0, y: 0 });
	const [circles, setCircles] = useState([{ id: 1, x: 0, y: 0 }]);
	const [draggingCanvas, setDraggingCanvas] = useState(false);
	const [draggingCircle, setDraggingCircle] = useState(null);
	const [dragStart, setDragStart] = useState({ x: 0, y: 0 });

	const handleWheel = (e: React.WheelEvent) => {
		const sensitivity = 0.0005;
		const deltaScale = 1 + e.deltaY * sensitivity;
		const newScale = Math.max(0.1, Math.min(3, scale * deltaScale));
		setScale(newScale);
	};

	const handleCanvasMouseDown = (e: React.MouseEvent) => {
		setDraggingCanvas(true);
		setDragStart({ x: e.clientX - pan.x, y: e.clientY - pan.y });
	};

	const handleCircleMouseDown = (e: React.MouseEvent, circle) => {
		e.stopPropagation();
		setDraggingCircle(circle);
		setDragStart({ x: e.clientX - circle.x, y: e.clientY - circle.y });
	};

	const handleMouseMove = (e: React.MouseEvent) => {
		if (draggingCanvas) {
			const newX = e.clientX - dragStart.x;
			const newY = e.clientY - dragStart.y;
			setPan({ x: newX, y: newY });
		}

		if (draggingCircle) {
			const updatedCircles = circles.map((circle) =>
				circle.id === draggingCircle.id
					? {
							...circle,
							x: e.clientX - dragStart.x,
							y: e.clientY - dragStart.y,
						}
					: circle,
			);
			setCircles(updatedCircles);
		}
	};

	const handleMouseUp = () => {
		setDraggingCanvas(false);
		setDraggingCircle(null);
	};

	return (
		<div
			className="zoomable-canvas border border-5"
			onWheel={handleWheel}
			onMouseDown={handleCanvasMouseDown}
			onMouseMove={handleMouseMove}
			onMouseUp={handleMouseUp}
		>
			<div
				className="canvas"
				ref={canvasRef}
				style={{
					transform: `scale(${scale}) translate(${pan.x}px, ${pan.y}px)`,
				}}
			>
				{circles.map((circle) => (
					<div
						key={circle.id}
						className="draggable-circle"
						onMouseDown={(e) => handleCircleMouseDown(e, circle)}
						style={{
							position: "absolute",
							left: `${circle.x}px`,
							top: `${circle.y}px`,
							transform: "translate(-50%, -50%)",
							width: "50px",
							height: "50px",
							borderRadius: "50%",
							background: "blue",
							cursor: "grab",
							userSelect: "none",
						}}
					></div>
				))}
			</div>
		</div>
	);
};

export default Canvas;
