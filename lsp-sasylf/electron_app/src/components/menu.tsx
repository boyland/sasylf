import React from "react";
import { Menu, Item, Separator, Submenu } from "react-contexify";
import "react-contexify/dist/ReactContexify.css";

function handleItemClick(event: any) {
	console.log(event);
}

function ContextMenu(props: { MENU_ID: string }) {
	return (
		<Menu id={props.MENU_ID}>
			<Item onClick={handleItemClick}>Menu {props.MENU_ID}</Item>
			<Item onClick={handleItemClick}>Replace free variables</Item>
			<Separator />
			<Item disabled>Disabled</Item>
			<Separator />
			<Submenu label="Submenu">
				<Item onClick={handleItemClick}>Sub Item 1</Item>
				<Item onClick={handleItemClick}>Sub Item 2</Item>
			</Submenu>
		</Menu>
	);
}

export default ContextMenu;
