from PyQt6.QtWidgets import QLabel
from PyQt6.QtGui import QFont, QMouseEvent, QColor
from PyQt6.QtCore import (
    QParallelAnimationGroup,
    QSequentialAnimationGroup,
    Qt,
    QPropertyAnimation,
    pyqtProperty,
)
from PyQt6.QtCore import pyqtSignal


class CodeText(QLabel):
    def __init__(self, text: str):
        super(CodeText, self).__init__()

        font = QFont("Courier")
        font.setStyleHint(QFont.StyleHint.TypeWriter)
        self.setFont(font)

        self.setText(text)


class JudgmentWidget(CodeText):
    def __init__(self, text: str):
        super(JudgmentWidget, self).__init__(text)

        self.setStyleSheet("border: 2px solid black; padding: 10px;")
        self.setAlignment(Qt.AlignmentFlag.AlignCenter)


class RuleWidget(CodeText):
    clicked = pyqtSignal(CodeText)

    def __init__(self, text: str):
        super(RuleWidget, self).__init__(text)

        self.setStyleSheet(
            f"""
            border: 2px solid black;
            border-radius: 5px;
            padding: 10px;
            """
        )
        self.setCursor(Qt.CursorShape.PointingHandCursor)

        self.colorVal = QColor("white")
        self.textColorVal = QColor("black")

        def makeProperty(property: bytes, col1: str, col2: str, duration: int = 250):
            prop = QPropertyAnimation(self, property)
            prop.setStartValue(QColor(col1))
            prop.setEndValue(QColor(col2))
            prop.setDuration(duration)

            return prop

        colorAnim = makeProperty(b"color", "white", "black")
        textColorAnim = makeProperty(b"textColor", "black", "white")
        clickForwardAnim = makeProperty(b"color", "black", "green", 400)
        clickBackwardAnim = makeProperty(b"color", "green", "black", 400)

        self.animGroup = QParallelAnimationGroup(self)
        self.animGroup.addAnimation(colorAnim)
        self.animGroup.addAnimation(textColorAnim)

        self.clickGroup = QSequentialAnimationGroup(self)
        self.clickGroup.addAnimation(clickForwardAnim)
        self.clickGroup.addAnimation(clickBackwardAnim)

    @pyqtProperty(QColor)
    def color(self) -> QColor:
        return self.colorVal

    @color.setter
    def color(self, val: QColor):
        self.colorVal = val
        self.setStyleSheet(
            f"""
            border: 2px solid black;
            border-radius: 5px;
            padding: 10px;
            background-color: {val.name()};
            color: {self.textColorVal.name()};
            """
        )

    @pyqtProperty(QColor)
    def textColor(self) -> QColor:
        return self.textColorVal

    @textColor.setter
    def textColor(self, val: QColor):
        self.textColorVal = val
        self.setStyleSheet(
            f"""
            border: 2px solid black;
            border-radius: 5px;
            padding: 10px;
            background-color: {self.colorVal.name()};
            color: {val.name()};
            """
        )

    def enterEvent(self, _):
        self.animGroup.stop()
        self.animGroup.setDirection(self.animGroup.Direction.Forward)
        self.animGroup.start()

    def leaveEvent(self, _):
        self.animGroup.stop()
        self.animGroup.setDirection(self.animGroup.Direction.Backward)
        self.animGroup.start()

    def mousePressEvent(self, e: QMouseEvent):
        if e.button() == Qt.MouseButton.LeftButton:
            self.clickGroup.stop()
            self.clickGroup.start()
            self.clicked.emit(self)
