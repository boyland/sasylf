from PyQt6.QtWidgets import QMainWindow, QTextEdit, QVBoxLayout
from PyQt6.QtCore import pyqtSlot
from widgets.util_widgets import JudgmentWidget, RuleWidget


def judgmentToText(judgment: dict) -> str:
    return f"judgment {judgment['name']}: {judgment['form']}"


def ruleToText(rule: dict) -> str:
    max_len = 0

    for premise in rule["premises"]:
        max_len = max(max_len, len(premise))

    max_len = max(max_len, len(rule["conclusion"]))
    res = ""

    for premise in rule["premises"]:
        res += f"{premise}\n"

    res += f"{'-' * max_len} {rule['name']}\n"
    res += rule["conclusion"]

    return res


class Judgment(QVBoxLayout):
    def __init__(self, judgment: dict):
        super(Judgment, self).__init__()

        self.addWidget(JudgmentWidget(judgmentToText(judgment)))
        rules = []

        for rule in judgment["rules"]:
            rules.append(RuleWidget(ruleToText(rule)))
            rules[-1].clicked.connect(self.on_click)
            self.addWidget(rules[-1])

    @pyqtSlot(RuleWidget)
    def on_click(self, sender: RuleWidget):
        mainWindow = self

        for _ in range(6):
            assert mainWindow is not None
            mainWindow = mainWindow.parent()

        assert mainWindow is not None
        assert isinstance(mainWindow, QMainWindow)

        textEdit = mainWindow.centralWidget()

        assert isinstance(textEdit, QTextEdit)

        textEdit.setText(sender.text())
