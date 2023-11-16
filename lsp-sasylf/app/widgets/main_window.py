from PyQt6.QtWidgets import (
    QMainWindow,
    QWidget,
    QDockWidget,
    QTextEdit,
    QVBoxLayout,
    QScrollArea,
    QTabWidget,
)
from PyQt6.QtCore import Qt
from widgets.judgment import Judgment
from widgets.util_widgets import Tabs
import json


class MainWindow(QMainWindow):
    def __init__(self, text: str):
        super(MainWindow, self).__init__()

        ast = json.loads(text)

        self.resize(800, 600)
        self.setStyleSheet("background-color: white; color: black;")
        self.setWindowTitle("Derivation Builder")

        rulesDock = QDockWidget("Rules", self)
        self.addDockWidget(Qt.DockWidgetArea.RightDockWidgetArea, rulesDock)

        scrollArea = QScrollArea(self)
        scrollArea.setWidgetResizable(True)

        dockContent = QWidget(self)
        dockMainLayout = QVBoxLayout(dockContent)

        for judgment in ast["judgments"]:
            dockMainLayout.addLayout(Judgment(judgment))

        scrollArea.setWidget(dockContent)
        rulesDock.setWidget(scrollArea)

        tabWidget = Tabs(self)

        mainWidget = QTextEdit()
        tabWidget.addTab(mainWidget, "Tab 1")
        self.setCentralWidget(tabWidget)
