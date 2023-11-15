from PyQt6.QtWidgets import (
    QMainWindow,
    QWidget,
    QDockWidget,
    QTextEdit,
    QVBoxLayout,
    QScrollArea,
)
from PyQt6.QtCore import Qt
from widgets.judgment import Judgment
import json


class MainWindow(QMainWindow):
    def __init__(self, text: str):
        super(MainWindow, self).__init__()

        ast = json.loads(text)

        self.resize(800, 600)
        self.setStyleSheet("background-color: white; color: black;")
        self.setWindowTitle("Derivation Builder")

        rules_dock = QDockWidget("Rules", self)
        self.addDockWidget(Qt.DockWidgetArea.RightDockWidgetArea, rules_dock)

        scroll_area = QScrollArea(self)
        scroll_area.setWidgetResizable(True)

        dock_content = QWidget(self)
        dock_main_layout = QVBoxLayout(dock_content)

        for judgment in ast["judgments"]:
            dock_main_layout.addLayout(Judgment(judgment))

        scroll_area.setWidget(dock_content)
        rules_dock.setWidget(scroll_area)

        main_widget = QTextEdit()
        self.setCentralWidget(main_widget)
