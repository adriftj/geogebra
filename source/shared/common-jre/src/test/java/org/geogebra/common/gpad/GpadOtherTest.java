package org.geogebra.common.gpad;

import org.geogebra.common.BaseUnitTest;
import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integrated tests for actual Gpad script.
 */
public class GpadOtherTest extends BaseUnitTest {
	/**
	 * Test the actual Gpad script.
	 */
	@Test
	public void testAbsolute_Value_Functions_as_Piecewise_Functions() {
		String gpad =
			"@@macro PixelsToPoint(Corner1, Corner3, Corner5, InputPixels) {\n" +
			"    @InputPixelsStyle = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 100.0 200.0 }\n" +
			"    InputPixels @InputPixelsStyle = (100, 200);\n" +
			"    @Corner5Style = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 685.0 508.0 }\n" +
			"    Corner5 @Corner5Style = (685, 508);\n" +
			"    @Corner3Style = { objColor: #4D4DFF00; animation: +; coords: 8.980000000000018 6.100000000000002 }\n" +
			"    Corner3 @Corner3Style = (8.98, 6.1);\n" +
			"    xRight = x(Corner3);\n" +
			"    @Corner1Style = { objColor: #4D4DFF00; animation: +; coords: -4.760000000000007 -4.099999999999998 }\n" +
			"    Corner1 @Corner1Style = (-4.76, -4.1);\n" +
			"    xLeft = x(Corner1);\n" +
			"    yTop = y(Corner3);\n" +
			"    yBottom = y(Corner1);\n" +
			"    yCoord = yTop - y(InputPixels) / y(Corner5) (yTop - yBottom);\n" +
			"    xCoord = x(InputPixels) / x(Corner5) (xRight - xLeft) + xLeft;\n" +
			"    @OutputPointStyle = { show: 3d; objColor: #44444400; pointSize: 4; coords: -2.7541605839416095 2.0842519685039393 }\n" +
			"    OutputPoint @OutputPointStyle = (xCoord, yCoord);\n" +
			"    @@return OutputPoint\n" +
			"}\n" +
			"\n" +
			"@@macro expandSegment(A, B, pixelGap, Corner1, Corner3, Corner5) {\n" +
			"    @Corner5Style = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 568.0 443.0 }\n" +
			"    Corner5* @Corner5Style = (568, 443);\n" +
			"    @Corner3Style = { objColor: #00000000; labelMode: namevalue; animation: +; coords: 1.2547483093257106 4.757128434755219 }\n" +
			"    Corner3* @Corner3Style = (1.25, 4.76);\n" +
			"    @Corner1Style = { show: 3d; objColor: #4D4DFF00; animation: +; coords: -0.6147829327656786 -9.838334771045973 }\n" +
			"    Corner1* @Corner1Style = (-0.61, -9.84);\n" +
			"    @pixelGapStyle = { labelOffset: -79 10 }\n" +
			"    pixelGap @pixelGapStyle = 12.1;\n" +
			"    distanceX = pixelGap / x(Corner5) (x(Corner3) - x(Corner1));\n" +
			"    distanceY = pixelGap / y(Corner5) (y(Corner3) - y(Corner1));\n" +
			"    pointGap = distanceX;\n" +
			"    xyScale = distanceY / distanceX;\n" +
			"    @BStyle = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 0.028073424163992024 -2.3274110089595177 }\n" +
			"    B @BStyle = (0.03, -2.33);\n" +
			"    @AStyle = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 0.0 0.0 }\n" +
			"    A @AStyle = (0, 0);\n" +
			"    @vecABStyle = { show: 3d; objColor: #00000000; lineStyle: full thickness=5 hidden=dashed opacity=178; coords: 0.028073424163992024 -2.3274110089595177 0.0 }\n" +
			"    vecAB* @vecABStyle = B - A;\n" +
			"    @pVecStyle = { show: 3d; objColor: #33FF0000; labelOffset: 80 -6; lineStyle: full thickness=5 hidden=dashed; coords: 12.749697433362456 15.409257456402978 0.0 }\n" +
			"    pVec* @pVecStyle = 20UnitVector[((-y(vecAB)) / xyScale, x(vecAB) xyScale)];\n" +
			"    @pVecAngleStyle = { lineStyle: full thickness=5 hidden=show opacity=178; show: 3d; objColor: #0064001A; bgColor: #000000FF; labelOffset: 47 -65; labelMode: namevalue }\n" +
			"    pVecAngle @pVecAngleStyle = Angle[xAxis, Line[(0, 0), pVec]];\n" +
			"    realAngle = If[pVecAngle ≟ π / 2 ∨ pVecAngle ≟ 3π / 2, pVecAngle, atan(tan(pVecAngle) / xyScale)];\n" +
			"    @curveFunctionStyle = { show: 3d; objColor: #00000000; labelOffset: 60 -15; lineStyle: full thickness=5 hidden=dashed opacity=178 }\n" +
			"    curveFunction* @curveFunctionStyle = CurveCartesian[pointGap cos(u), pointGap xyScale sin(u), u, 0, 2π];\n" +
			"    @KStyle = { show: 3d; objColor: #44444400; pointSize: 4; coords: 0.03953911977970617 0.04778689686319611 }\n" +
			"    K @KStyle = curveFunction(realAngle);\n" +
			"    @vSpaceStyle = { objColor: #00000000; lineStyle: full thickness=5 hidden=dashed opacity=178; coords: 0.03953911977970617 0.04778689686319611 0.0 }\n" +
			"    vSpace* @vSpaceStyle = Vector[K];\n" +
			"    @allPointsStyle = { show: 3d; objColor: #00640000; lineStyle: full thickness=5 hidden=dashed }\n" +
			"    allPoints @allPointsStyle = {A + vSpace, A - vSpace, B - vSpace, B + vSpace};\n" +
			"    @newPolyStyle = { lineStyle: full thickness=5 hidden=dashed opacity=178; objColor: #9933001A }\n" +
			"    newPoly~ @newPolyStyle = Polygon[allPoints];\n" +
			"    @@return newPoly\n" +
			"}\n" +
			"\n" +
			"@@macro onboardingTextOffset(CornerText1, CornerText3, case, Corner1, Corner3, Corner5) {\n" +
			"    @CornerText1Style = { objColor: #44444400; animation: +; pointSize: 4; coords: 1.32 2.06 }\n" +
			"    CornerText1 @CornerText1Style = (1.32, 2.06);\n" +
			"    @CornerText3Style = { show: 3d; objColor: #44444400; animation: +; pointSize: 4; coords: 3.22 2.54 }\n" +
			"    CornerText3 @CornerText3Style = (3.22, 2.54);\n" +
			"    @Corner1Style = { objColor: #44444400; animation: +; pointSize: 4; coords: -4.32 -2.58 }\n" +
			"    Corner1 @Corner1Style = (-4.32, -2.58);\n" +
			"    @Corner3Style = { objColor: #44444400; animation: +; pointSize: 4; coords: 7.08 6.32 }\n" +
			"    Corner3 @Corner3Style = (7.08, 6.32);\n" +
			"    @Corner5Style = { objColor: #44444400; animation: +; pointSize: 4; coords: 568.0 443.0 }\n" +
			"    Corner5 @Corner5Style = (568, 443);\n" +
			"    case = 4;\n" +
			"    xPixels = (x(CornerText3) - x(CornerText1)) x(Corner5) / (x(Corner3) - x(Corner1));\n" +
			"    yPixels = (y(CornerText3) - y(CornerText1)) y(Corner5) / (y(Corner3) - y(Corner1));\n" +
			"    xOffset = Element[{27, -xPixels + 2, 16 - xPixels / 2, 16 - xPixels / 2}, case];\n" +
			"    yOffset = Element[{-23, -23, yPixels - 27, -49}, case];\n" +
			"    @TextOffsetStyle = { objColor: #44444400; pointSize: 4; coords: -31.33333333333333 -49.0 }\n" +
			"    TextOffset @TextOffsetStyle = (xOffset, yOffset);\n" +
			"    @@return TextOffset\n" +
			"}\n" +
			"\n" +
			"@@macro expandCircle(A, pixelRadius, Corner1, Corner3, Corner5) {\n" +
			"    pixelRadius = 14;\n" +
			"    @Corner1Style = { objColor: #4D4DFF00; animation: +; coords: -12.0 -11.875 }\n" +
			"    Corner1 @Corner1Style = (-12, -11.88);\n" +
			"    @Corner3Style = { objColor: #4D4DFF00; animation: +; coords: 31.0 15.0 }\n" +
			"    Corner3 @Corner3Style = (31, 15);\n" +
			"    @Corner5Style = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 800.0 500.0 }\n" +
			"    Corner5 @Corner5Style = (800, 500);\n" +
			"    @distanceXStyle = { symbolic }\n" +
			"    distanceX @distanceXStyle = pixelRadius / x(Corner5) (x(Corner3) - x(Corner1));\n" +
			"    @distanceYStyle = { symbolic }\n" +
			"    distanceY @distanceYStyle = pixelRadius / y(Corner5) (y(Corner3) - y(Corner1));\n" +
			"    xyScale = distanceY / (distanceX);\n" +
			"    pointGap = distanceX;\n" +
			"    cMinor = Min[pointGap, pointGap xyScale];\n" +
			"    cMajor = Max[pointGap, pointGap xyScale];\n" +
			"    f1 = sqrt(cMajor² - cMinor²);\n" +
			"    @AStyle = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 0.0 0.0 }\n" +
			"    A @AStyle = (0, 0);\n" +
			"    @newEllipseStyle = { show: 3d; objColor: #00000000; lineStyle: full thickness=5 hidden=dashed opacity=178; eqnStyle: implicit }\n" +
			"    newEllipse @newEllipseStyle = If[xyScale < 1, Ellipse[A + (f1, 0), A + (-f1, 0), A + (cMajor, 0)], Ellipse[A + (0, f1), A + (0, -f1), A + (0, cMajor)]];\n" +
			"    @@return newEllipse\n" +
			"}\n" +
			"\n" +
			"@@macro PointToPixels(Corner1, Corner3, Corner5, InputPoint) {\n" +
			"    @Corner1Style = { objColor: #4D4DFF00; animation: +; coords: -12.0 -11.875 }\n" +
			"    Corner1 @Corner1Style = (-12, -11.88);\n" +
			"    @Corner3Style = { objColor: #4D4DFF00; animation: +; coords: 31.0 15.0 }\n" +
			"    Corner3 @Corner3Style = (31, 15);\n" +
			"    @Corner5Style = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 800.0 500.0 }\n" +
			"    Corner5 @Corner5Style = (800, 500);\n" +
			"    @InputPointStyle = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 0.0 -1.0 }\n" +
			"    InputPoint @InputPointStyle = (0, -1);\n" +
			"    xLeft = x(Corner1);\n" +
			"    xRight = x(Corner3);\n" +
			"    yTop = y(Corner3);\n" +
			"    yBottom = y(Corner1);\n" +
			"    yPixels = y(Corner5) (yTop - y(InputPoint)) / (yTop - yBottom);\n" +
			"    xPixels = x(Corner5) (x(InputPoint) - xLeft) / (xRight - xLeft);\n" +
			"    @OutputPixelsStyle = { show: 3d; objColor: #44444400; pointSize: 4; coords: 223.25581395348837 297.6744186046512 }\n" +
			"    OutputPixels @OutputPixelsStyle = (xPixels, yPixels);\n" +
			"    @@return OutputPixels\n" +
			"}\n" +
			"\n" +
			"@@macro asCoeff(coeff) {\n" +
			"    coeff = 0;\n" +
			"    @prefixStyle = { objColor: #00000000 }\n" +
			"    prefix @prefixStyle = If[coeff < 0, \"-\", \"+\"];\n" +
			"    @coeffStringStyle = { objColor: #00000000; startPoint: 2.0499999999999994 2.5600000000000014 1.0 }\n" +
			"    coeffString @coeffStringStyle = prefix If[abs(coeff) ≟ 1, \"\", \"\" abs(coeff)];\n" +
			"    @@return coeffString\n" +
			"}\n" +
			"\n" +
			"@@macro addConstantString(myNum) {\n" +
			"    myNum = 7;\n" +
			"    @outputStringStyle = { objColor: #00000000 }\n" +
			"    outputString @outputStringStyle = If[myNum ≟ 0, \"\", If[myNum < 0, \"-\", \"+\"] abs(myNum)];\n" +
			"    @@return outputString\n" +
			"}\n" +
			"\n" +
			"@@macro addColoredConstant(myNum, color) {\n" +
			"    myNum = 0;\n" +
			"    @colorStyle = { objColor: #00000000; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"    color @colorStyle = \"#6557D2\";\n" +
			"    @outputStringStyle = { show: ~3d; objColor: #00000000; isLaTeX; decimals: 2; startPoint: 0.0 0.0 1.0 }\n" +
			"    outputString @outputStringStyle = If[myNum ≟ 0, \"\", If[myNum < 0, \"-\", \"+\"] \"\\textcolor{\" color \"}{\" abs(myNum)] \"}\";\n" +
			"    @@return outputString\n" +
			"}\n" +
			"\n" +
			"@onboardingStyle = { objColor: #00000000; checkbox; fixed }\n" +
			"onboarding* @onboardingStyle = true;\n" +
			"@colorGreyDarkStyle = { show: ~3d; objColor: #4D4D4D00; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorGreyDark* @colorGreyDarkStyle = \"#4D4D4D\";\n" +
			"tries = 1;\n" +
			"@moveBoxStyle = { lineStyle: full thickness=7 hidden=dashed; show: 3d; objColor: #1565C01A; layer: 4 }\n" +
			"moveBox* @moveBoxStyle = Polygon[{(-9, -10), (9, -10), (9, 10), (-9, 10)}];\n" +
			"@PVertexStyle = { show: 3d; objColor: #6557D200; layer: 3; animation: +1; pointSize: 7; pointStyle: no_outline; coords: 0.0 0.0 }\n" +
			"PVertex~ @PVertexStyle = PointIn[moveBox];\n" +
			"xPosition = x(PVertex);\n" +
			"@PDilateStyle = { show: 3d; objColor: #DA257000; layer: 5; animation: +1; pointSize: 7; pointStyle: no_outline; coords: 1.0 1.0 }\n" +
			"PDilate~ @PDilateStyle = (1, 1);\n" +
			"@scriptResetAppStyle = { objColor: #1565C000; bgColor: #FFFFFFFF; layer: 3; labelOffset: 569 1; fixed; auxiliary; caption: \"Reset app\"; font: bold }\n" +
			"scriptResetApp* @scriptResetAppStyle = Button(\"Reset app\");\n" +
			"@helpTextHintTypicalErrorStyle = { show: ~3d; objColor: #4D4D4D00; layer: 3; isLaTeX; @screen: 11 247 }\n" +
			"helpTextHintTypicalError* @helpTextHintTypicalErrorStyle = \"\\text{This is the error specific hint}\";\n" +
			"@colorTealSuccessStyle = { show: ~3d; objColor: #00675800; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorTealSuccess* @colorTealSuccessStyle = \"#006758\";\n" +
			"@colorRedErrorStyle = { show: ~3d; objColor: #B0002000; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorRedError* @colorRedErrorStyle = \"#B00020\";\n" +
			"@colorBlackStyle = { show: ~3d; objColor: #25252500; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorBlack* @colorBlackStyle = \"#252525\";\n" +
			"@colorGreyMediumStyle = { show: ~3d; objColor: #75757500; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorGreyMedium* @colorGreyMediumStyle = \"#757575\";\n" +
			"@colorGreyGridStyle = { show: ~3d; objColor: #94949400; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorGreyGrid* @colorGreyGridStyle = \"#949494\";\n" +
			"@colorRedLightStyle = { show: ~3d; objColor: #E4446100; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorRedLight* @colorRedLightStyle = \"#E44461\";\n" +
			"@colorPinkStyle = { show: ~3d; objColor: #DA257000; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorPink* @colorPinkStyle = \"#DA2570\";\n" +
			"@colorPinkLightStyle = { show: ~3d; objColor: #E66B9E00; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorPinkLight* @colorPinkLightStyle = \"#E66B9E\";\n" +
			"@colorBlueStyle = { show: ~3d; objColor: #3A6ED600; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorBlue* @colorBlueStyle = \"#3A6ED6\";\n" +
			"@colorBlueLightStyle = { show: ~3d; objColor: #5991FF00; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorBlueLight* @colorBlueLightStyle = \"#5991FF\";\n" +
			"@colorTealLightStyle = { show: ~3d; objColor: #00847500; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorTealLight* @colorTealLightStyle = \"#008475\";\n" +
			"@colorGreenStyle = { show: ~3d; objColor: #0F853800; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorGreen* @colorGreenStyle = \"#0F8538\";\n" +
			"@colorGreenLightStyle = { show: ~3d; objColor: #00A83C00; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorGreenLight* @colorGreenLightStyle = \"#00a83c\";\n" +
			"@colorPurpleDarkStyle = { show: ~3d; objColor: #5145A800; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorPurpleDark* @colorPurpleDarkStyle = \"#5145A8\";\n" +
			"@colorPurpleButtonStyle = { show: ~3d; objColor: #6557D200; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorPurpleButton* @colorPurpleButtonStyle = \"#6557D2\";\n" +
			"@colorPurpleLightStyle = { show: ~3d; objColor: #8575FF00; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorPurpleLight* @colorPurpleLightStyle = \"#8575FF\";\n" +
			"@colorBrownStyle = { show: ~3d; objColor: #903D1400; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorBrown* @colorBrownStyle = \"#903D14\";\n" +
			"@colorBrownLightStyle = { show: ~3d; objColor: #BD6C4300; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorBrownLight* @colorBrownLightStyle = \"#BD6C43\";\n" +
			"@colorOrangeStyle = { show: ~3d; objColor: #C7500000; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorOrange* @colorOrangeStyle = \"#C75000\";\n" +
			"@colorOrangeLightStyle = { show: ~3d; objColor: #E0741500; layer: 3; auxiliary; startPoint: 5.33882319942803 6.630892454837512 1.0 }\n" +
			"colorOrangeLight* @colorOrangeLightStyle = \"#E07415\";\n" +
			"@helpTextHintStyle = { show: ~3d; objColor: #4D4D4D00; layer: 3; isLaTeX; @screen: 12 317 }\n" +
			"helpTextHint* @helpTextHintStyle = \"\\textsf{The graph of }p(x)=\\vert{x}\\vert \\textsf{ is shown.} \\\\\n" +
			"\\textsf{Transform the function with}\\\\\n" +
			"\\textsf{a dilation and a translation.}\";\n" +
			"pixelsRight = 16;\n" +
			"pixelsLeft = 16;\n" +
			"pixelsBottom = 16;\n" +
			"pixelsTop = 24;\n" +
			"xMax = 31;\n" +
			"xMin = -12;\n" +
			"yMax = 15;\n" +
			"@yMinStyle = { symbolic }\n" +
			"yMin @yMinStyle = yMax - (xMax - xMin) * 5 / 8;\n" +
			"@colorInactiveButtonStyle = { show: ~3d; objColor: #C0C0C000; layer: 4; startPoint: 4.791457979391604 5.2966048644938475 1.0 }\n" +
			"colorInactiveButton* @colorInactiveButtonStyle = \"#eeeeee\";\n" +
			"@colorInputCorrectStyle = { show: ~3d; objColor: #00000000; layer: 4; startPoint: 4.791457979391604 5.2966048644938475 1.0 }\n" +
			"colorInputCorrect* @colorInputCorrectStyle = \"#2E006758\";\n" +
			"@colorInputIncorrectStyle = { show: ~3d; objColor: #00000000; layer: 4; startPoint: 4.791457979391604 5.2966048644938475 1.0 }\n" +
			"colorInputIncorrect* @colorInputIncorrectStyle = \"#2EB00020\";\n" +
			"layoutY = 250;\n" +
			"layoutX = 455;\n" +
			"pulsingSpeed = 8;\n" +
			"@pulsingStyle = { slider: min=0 max=1 width=200.0 @screen algebra; lineStyle: full thickness=8 hidden=dashed; objColor: #0000001A; bgColor: #000000FF; layer: 4; labelMode: namevalue; animation: + speed=\"pulsingSpeed\" }\n" +
			"pulsing* @pulsingStyle = Slider(0.0, 1.0);\n" +
			"@onboardingCaseStyle = { slider: min=1 max=4 width=200.0 x=266.0 y=350.0 @screen; lineStyle: full thickness=8 hidden=dashed; objColor: #0000001A; bgColor: #000000FF; layer: 4; labelMode: namevalue; animation: 1 }\n" +
			"onboardingCase* @onboardingCaseStyle = Slider(1.0, 4.0);\n" +
			"@layoutAppSizeStyle = { show: 3d; objColor: #52525200; layer: 4; animation: +1; pointSize: 7; coords: 800.0 500.0 }\n" +
			"layoutAppSize* @layoutAppSizeStyle = Point[{800, 500}];\n" +
			"@onboardingPXStyle = { show: 3d; objColor: #44444400; layer: 4; pointSize: 4; coords: 223.25581395348837 297.6744186046512 }\n" +
			"onboardingPX* @onboardingPXStyle = PointToPixels[(xMin, yMin), (xMax, yMax), layoutAppSize, (0, -1)];\n" +
			"onboardingX = x(onboardingPX) - 12;\n" +
			"onboardingY = y(onboardingPX) + 36;\n" +
			"@onboardingArrowUpStyle = { showIf: onboarding; startPoint: absolute \"(onboardingX, onboardingY)\" }\n" +
			"onboardingArrowUp @onboardingArrowUpStyle = Image(\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==\");\n" +
			"@altTextStyle = { show: ~3d; objColor: #00000000; layer: 4; isLaTeX; startPoint: 6.422187344952864 4.251074692411592 1.0 }\n" +
			"altText* @altTextStyle = \"\\text{Describe the applet here}\";\n" +
			"@inputCStyle = { show: ~3d; objColor: #00000000; layer: 4; startPoint: 6.198556216753847 2.2101079015668352 1.0 }\n" +
			"inputC* @inputCStyle = \"\";\n" +
			"@onboardingPositionsStyle = { caption: \"(Element 1, element 2) = pixel location of text, element 3: text direction for x: 1=right of point, -1=left of point, 0=centered. element 4: text direction for y (-1,0,1)\" }\n" +
			"onboardingPositions @onboardingPositionsStyle = Element[{{onboardingX + 24, onboardingY - 12, 1, 0}, {onboardingX, onboardingY - 12, -1, 0}, {onboardingX - 18, onboardingY + 18, 0, -1}, {onboardingX + 18, onboardingY - 36, 0, 0}}, onboardingCase];\n" +
			"@viewboxStyle = { lineStyle: full thickness=0 hidden=dashed opacity=0; show: 3d; objColor: #FFFFFF inverse; layer: 3 }\n" +
			"viewbox~ @viewboxStyle = Polygon[{(-11, 11), (-11, -11), (11, -11), (11, 11)}];\n" +
			"@pulsingPointStyle = { show: 3d; showIf: onboarding; objColor: rgb(0.396078431372549,0.3411764705882353,0.8235294117647058,\"1 - pulsing\"); layer: 4; lineStyle: full thickness=1 hidden=dashed opacity=0; eqnStyle: implicit }\n" +
			"pulsingPoint~ @pulsingPointStyle = expandCircle[PVertex, 7 + 7pulsing, (xMin, yMin), (xMax, yMax), (800, 500)];\n" +
			"@onboardingBackgroundStyle = { lineStyle: full thickness=0 hidden=dashed opacity=0; show: 3d; showIf: onboarding; objColor: #FFFFFF; layer: 2 }\n" +
			"onboardingBackground~ @onboardingBackgroundStyle = Polygon[{(-0.9, -3.5), (0.9, -3.5), (0.9, -1.5), (-0.9, -1.5)}];\n" +
			"@axisXStyle = { show: 3d; objColor: #25252500; layer: 1; lineStyle: full thickness=4 hidden=dashed; eqnStyle: implicit; keepTypeOnTransform; startStyle: arrow; endStyle: arrow; coords: 0.0 21.9 -0.0 }\n" +
			"axisX~ @axisXStyle = Segment[(-10.95, 0), (10.95, 0)];\n" +
			"@axisYStyle = { show: 3d; objColor: #25252500; layer: 1; lineStyle: full thickness=4 hidden=dashed; eqnStyle: implicit; keepTypeOnTransform; startStyle: arrow; endStyle: arrow; coords: -21.9 0.0 0.0 }\n" +
			"axisY~ @axisYStyle = Segment[(0, -10.95), (0, 10.95)];\n" +
			"@textQuestionStyle = { symbolic; show: ~3d; objColor: #25252500; layer: 3; fixed; isLaTeX; font: serif; @screen: 11 20 }\n" +
			"textQuestion @textQuestionStyle = \"\\textsf{Drag the points to change the graph of }f(x)\\textsf{ and observe how the equation is affected.}\";\n" +
			"@onboardingTextStyle = { show: ~3d; showIf: onboarding; objColor: #6557D200; bgColor: #FFFFFFFF; layer: 4; isLaTeX; startPoint: 0.0 -3.0 1.0 }\n" +
			"onboardingText~ @onboardingTextStyle = Text[\"\\text{Drag to start}\", (0, -3), true, true, 0, -1];\n" +
			"dilationAmount = 1;\n" +
			"@studentFuncStyle = { show: 3d; objColor: #5145A800; labelOffset: 52 61; fixed; lineStyle: full thickness=7 hidden=dashed }\n" +
			"studentFunc(x)~ @studentFuncStyle = dilationAmount abs(x - x(PVertex)) + y(PVertex);\n" +
			"@commonLatexStyle = { show: ~3d; objColor: #00000000; layer: 5; isLaTeX; font: serif; startPoint: 11.272839506172842 3.9505903490759753 1.0 }\n" +
			"commonLatex* @commonLatexStyle = \"\" + (LaTeX[If[x(PVertex) ≟ 0, \"x\", \"(x\" addColoredConstant[-x(PVertex), colorPurpleButton] \")\"]]) + \" \" + (LaTeX[If[y(PVertex) ≠ 0, addColoredConstant[y(PVertex), colorPurpleButton], \"\"]]) + \"\";\n" +
			"@noAbsValueStyle = { show: ~3d; showIf: \"y(PDilate) ≟ y(PVertex)\"; objColor: #00000000; layer: 5; isLaTeX; font: serif; labelOffset: 400 77; startPoint: absolute \"(layoutX, layoutY)\" }\n" +
			"noAbsValue @noAbsValueStyle = \"\\(f(x)=\" + (LaTeX[x(PVertex)]) + \"\\)\\textsf{ is a line, }\\\\\\textsf{not an absolute value function.}\";\n" +
			"@gridBorderStyle = { lineStyle: full thickness=2 hidden=dashed; show: 3d; objColor: #94949400 }\n" +
			"gridBorder~ @gridBorderStyle = Polygon[{(-11, 11), (-11, -11), (11, -11), (11, 11)}];\n" +
			"@lblXStyle = { show: ~3d; objColor: #25252500; layer: 5; isLaTeX; font: serif bold; startPoint: 11.0 0.1 1.0 }\n" +
			"lblX~ @lblXStyle = Text[\"x\", (11, 0.1), true, true, 1, 0];\n" +
			"@lblYStyle = { show: ~3d; objColor: #25252500; layer: 5; isLaTeX; font: serif bold; startPoint: 0.0 11.0 1.0 }\n" +
			"lblY~ @lblYStyle = Text[\"y\", (0, 11), true, true, 0, 1];\n" +
			"@studentEqnStyle = { symbolic; show: ~3d; showIf: \"y(PDilate) ≠ y(PVertex)\"; objColor: #00000000; layer: 5; fixed; isLaTeX; font: serif; labelOffset: 450 250; startPoint: absolute \"(layoutX, layoutY)\" }\n" +
			"studentEqn~ @studentEqnStyle = \"f(x)=\\begin{cases}\" + (LaTeX[If[-dilationAmount < 0, \"-\", \"\"] If[abs(dilationAmount) > 1, \"\\textcolor{\" colorPink \"}{\" abs(dilationAmount) \"}\", \"\"]]) + \"\" + (LaTeX[commonLatex]) + \", &x\\lt \" + (LaTeX[x(PVertex)]) + \"\\\\\\textcolor{\" + (LaTeX[colorPink]) + \"}{\" + (LaTeX[If[dilationAmount < 0, \"-\", \"\"] If[abs(dilationAmount) > 1, \"\" abs(dilationAmount), \"\"]]) + \"}\" + (LaTeX[commonLatex]) + \", &x\\ge \" + (LaTeX[x(PVertex)]) + \"\n" +
			"\\end{cases}\";\n" +
			"@tabOrderStyle = { objColor: #00640000; layer: 5; lineStyle: full thickness=5 hidden=dashed; symbolic }\n" +
			"tabOrder* @tabOrderStyle = {textQuestion, PVertex, PDilate, studentEqn};\n";
		
		// Record the GgbAPI instance to use the same one for evalGpad and getLastWarning/getLastError
		org.geogebra.common.plugin.GgbAPI ggbApi = getApp().getGgbApi();
		
		try {
			String result = ggbApi.evalGpad(gpad);
			
			// Always check for errors and warnings using the same GgbAPI instance
			String lastError = ggbApi.getLastError();
			String lastWarning = ggbApi.getLastWarning();

			if (lastWarning != null && !lastWarning.isEmpty())
				System.err.println("Warning: \n" + lastWarning);
			if (lastError != null && !lastError.isEmpty())
				System.err.println("Error: \n" + lastError);
			assertNull(lastError);
			assertNull(lastWarning);
			assertNotNull(result);
			System.out.println("Result: " + result);
		} catch (Exception e) {
			// Print detailed error information
			System.err.println("Exception occurred: " + e.getMessage());
			e.printStackTrace();
			
			// Check for the specific error using the same GgbAPI instance
			String lastError = ggbApi.getLastError();
			String lastWarning = ggbApi.getLastWarning();

			if (lastWarning != null && !lastWarning.isEmpty())
				System.err.println("+Warning: \n" + lastWarning);
			if (lastError != null && !lastError.isEmpty())
				System.err.println("+Error: \n" + lastError);
			
			throw new AssertionError("Unexpected exception: " + e.getMessage(), e);
		}
	}

	/**
	 * Test that curveFunction correctly depends on pointGap when converting from XML.
	 * This test verifies that the topological sort correctly identifies dependencies
	 * from command input arguments containing expressions.
	 */
	@Test
	public void testCurveFunctionDependsOnPointGap() {
		// XML containing expandSegment macro with curveFunction that depends on pointGap
		String xmlMacro = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
			"<geogebra format=\"5.0\" version=\"5.0\" app=\"classic\" platform=\"d\" id=\"\">\n" +
			"<macro cmdName=\"expandSegment\" toolName=\"expandSegment\" toolHelp=\"expandSegment(A,B,pixelGap,Corner1,Corner3,Corner5)\" iconFile=\"\" showInToolBar=\"true\" copyCaptions=\"true\" viewId=\"1\">\n" +
			"<macroInput a0=\"A\" a1=\"B\" a2=\"pixelGap\" a3=\"Corner1\" a4=\"Corner3\" a5=\"Corner5\"/>\n" +
			"<macroOutput a0=\"newPoly\"/>\n" +
			"<construction title=\"\" author=\"\" date=\"\">\n" +
			"<element type=\"point\" label=\"Corner5\">\n" +
			"	<show object=\"false\" label=\"true\" ev=\"4\"/>\n" +
			"	<objColor r=\"77\" g=\"77\" b=\"255\" alpha=\"0\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelMode val=\"0\"/>\n" +
			"	<animation step=\"0.1\" speed=\"1\" type=\"1\" playing=\"false\"/>\n" +
			"	<pointSize val=\"5\"/>\n" +
			"	<pointStyle val=\"0\"/>\n" +
			"	<coords x=\"568\" y=\"443\" z=\"1\"/>\n" +
			"</element>\n" +
			"<element type=\"point\" label=\"Corner3\">\n" +
			"	<show object=\"false\" label=\"true\"/>\n" +
			"	<objColor r=\"0\" g=\"0\" b=\"0\" alpha=\"0\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelMode val=\"1\"/>\n" +
			"	<animation step=\"0.1\" speed=\"1\" type=\"1\" playing=\"false\"/>\n" +
			"	<pointSize val=\"5\"/>\n" +
			"	<pointStyle val=\"0\"/>\n" +
			"	<coords x=\"1.2547483093257106\" y=\"4.757128434755219\" z=\"1\"/>\n" +
			"</element>\n" +
			"<element type=\"point\" label=\"Corner1\">\n" +
			"	<show object=\"false\" label=\"true\" ev=\"4\"/>\n" +
			"	<objColor r=\"77\" g=\"77\" b=\"255\" alpha=\"0\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelMode val=\"0\"/>\n" +
			"	<animation step=\"0.1\" speed=\"1\" type=\"1\" playing=\"false\"/>\n" +
			"	<pointSize val=\"5\"/>\n" +
			"	<pointStyle val=\"0\"/>\n" +
			"	<coords x=\"-0.6147829327656786\" y=\"-9.838334771045973\" z=\"1\"/>\n" +
			"</element>\n" +
			"<element type=\"numeric\" label=\"pixelGap\">\n" +
			"	<value val=\"12.1\"/>\n" +
			"	<show object=\"true\" label=\"true\"/>\n" +
			"	<objColor r=\"0\" g=\"0\" b=\"0\" alpha=\"0.10000000149011612\"/>\n" +
			"	<bgColor r=\"0\" g=\"0\" b=\"0\" alpha=\"255\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelOffset x=\"-79\" y=\"10\"/>\n" +
			"	<labelMode val=\"1\"/>\n" +
			"	<animation speed=\"1\" type=\"0\" playing=\"false\"/>\n" +
			"</element>\n" +
			"<expression label=\"distanceX\" exp=\"(pixelGap / x(Corner5) * (x(Corner3) - x(Corner1)))\" />\n" +
			"<element type=\"numeric\" label=\"distanceX\">\n" +
			"	<value val=\"0.03982628174173558\"/>\n" +
			"	<objColor r=\"0\" g=\"0\" b=\"0\" alpha=\"0.10000000149011612\"/>\n" +
			"	<bgColor r=\"0\" g=\"0\" b=\"0\" alpha=\"255\"/>\n" +
			"</element>\n" +
			"<expression label=\"distanceY\" exp=\"(pixelGap / y(Corner5) * (y(Corner3) - y(Corner1)))\" />\n" +
			"<element type=\"numeric\" label=\"distanceY\">\n" +
			"	<value val=\"0.39865712142256077\"/>\n" +
			"	<objColor r=\"0\" g=\"0\" b=\"0\" alpha=\"0.10000000149011612\"/>\n" +
			"	<bgColor r=\"0\" g=\"0\" b=\"0\" alpha=\"255\"/>\n" +
			"</element>\n" +
			"<expression label=\"pointGap\" exp=\"distanceX\" />\n" +
			"<element type=\"numeric\" label=\"pointGap\">\n" +
			"	<value val=\"0.03982628174173558\"/>\n" +
			"	<objColor r=\"0\" g=\"0\" b=\"0\" alpha=\"0.10000000149011612\"/>\n" +
			"	<bgColor r=\"0\" g=\"0\" b=\"0\" alpha=\"255\"/>\n" +
			"</element>\n" +
			"<expression label=\"xyScale\" exp=\"distanceY / distanceX\" />\n" +
			"<element type=\"numeric\" label=\"xyScale\">\n" +
			"	<value val=\"10.009900597996115\"/>\n" +
			"	<objColor r=\"0\" g=\"0\" b=\"0\" alpha=\"0.10000000149011612\"/>\n" +
			"	<bgColor r=\"0\" g=\"0\" b=\"0\" alpha=\"255\"/>\n" +
			"</element>\n" +
			"<element type=\"point\" label=\"B\">\n" +
			"	<show object=\"true\" label=\"true\" ev=\"4\"/>\n" +
			"	<objColor r=\"77\" g=\"77\" b=\"255\" alpha=\"0\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelMode val=\"0\"/>\n" +
			"	<animation step=\"0.1\" speed=\"1\" type=\"1\" playing=\"false\"/>\n" +
			"	<pointSize val=\"5\"/>\n" +
			"	<pointStyle val=\"0\"/>\n" +
			"	<coords x=\"0.028073424163992024\" y=\"-2.3274110089595177\" z=\"1\"/>\n" +
			"</element>\n" +
			"<expression label=\"A\" exp=\"(0, 0)\" type=\"point\"/>\n" +
			"<element type=\"point\" label=\"A\">\n" +
			"	<show object=\"true\" label=\"true\" ev=\"4\"/>\n" +
			"	<objColor r=\"77\" g=\"77\" b=\"255\" alpha=\"0\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelMode val=\"0\"/>\n" +
			"	<animation step=\"0.1\" speed=\"1\" type=\"1\" playing=\"false\"/>\n" +
			"	<pointSize val=\"5\"/>\n" +
			"	<pointStyle val=\"0\"/>\n" +
			"	<coords x=\"0\" y=\"0\" z=\"1\"/>\n" +
			"</element>\n" +
			"<expression label=\"vecAB\" exp=\"B - A\" type=\"vector\" />\n" +
			"<element type=\"vector\" label=\"vecAB\">\n" +
			"	<show object=\"false\" label=\"true\" ev=\"4\"/>\n" +
			"	<objColor r=\"0\" g=\"0\" b=\"0\" alpha=\"0\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelMode val=\"0\"/>\n" +
			"	<lineStyle thickness=\"5\" type=\"0\" typeHidden=\"1\" opacity=\"178\"/>\n" +
			"	<coordStyle style=\"cartesian\"/>\n" +
			"	<coords x=\"0.028073424163992024\" y=\"-2.3274110089595177\" z=\"0\"/>\n" +
			"</element>\n" +
			"<expression label=\"pVec\" exp=\"(20 * UnitVector[(((-y(vecAB))) / xyScale, (x(vecAB) * xyScale))])\" type=\"vector\" />\n" +
			"<element type=\"vector\" label=\"pVec\">\n" +
			"	<show object=\"false\" label=\"true\" ev=\"4\"/>\n" +
			"	<objColor r=\"51\" g=\"255\" b=\"0\" alpha=\"0\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelOffset x=\"80\" y=\"-6\"/>\n" +
			"	<labelMode val=\"0\"/>\n" +
			"	<lineStyle thickness=\"5\" type=\"0\" typeHidden=\"1\"/>\n" +
			"	<coordStyle style=\"cartesian\"/>\n" +
			"	<coords x=\"12.749697433362456\" y=\"15.409257456402978\" z=\"0\"/>\n" +
			"</element>\n" +
			"<command name=\"Angle\">\n" +
			"	<input a0=\"xAxis\" a1=\"Line[(0, 0), pVec]\"/>\n" +
			"	<output a0=\"pVecAngle\"/>\n" +
			"</command>\n" +
			"<element type=\"angle\" label=\"pVecAngle\">\n" +
			"	<angleStyle val=\"0\"/>\n" +
			"	<value val=\"0.8795669256618033\"/>\n" +
			"	<lineStyle thickness=\"5\" type=\"0\" typeHidden=\"2\" opacity=\"178\"/>\n" +
			"	<arcSize val=\"30\"/>\n" +
			"	<show object=\"true\" label=\"true\" ev=\"4\"/>\n" +
			"	<objColor r=\"0\" g=\"100\" b=\"0\" alpha=\"0.10000000149011612\"/>\n" +
			"	<bgColor r=\"0\" g=\"0\" b=\"0\" alpha=\"255\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelOffset x=\"47\" y=\"-65\"/>\n" +
			"	<labelMode val=\"1\"/>\n" +
			"</element>\n" +
			"<command name=\"If\">\n" +
			"	<input a0=\"pVecAngle ≟ pi / 2 ∨ pVecAngle ≟ (3 * pi / 2)\" a1=\"pVecAngle\" a2=\"atand(tan(pVecAngle) / xyScale)\"/>\n" +
			"	<output a0=\"realAngle\"/>\n" +
			"</command>\n" +
			"<element type=\"angle\" label=\"realAngle\">\n" +
			"	<angleStyle val=\"3\"/>\n" +
			"	<value val=\"0.12015860297083836\"/>\n" +
			"	<objColor r=\"0\" g=\"100\" b=\"0\" alpha=\"0.10000000149011612\"/>\n" +
			"	<bgColor r=\"0\" g=\"0\" b=\"0\" alpha=\"255\"/>\n" +
			"</element>\n" +
			"<command name=\"CurveCartesian\">\n" +
			"	<input a0=\"(pointGap * cos(u))\" a1=\"((pointGap * xyScale) * sin(u))\" a2=\"u\" a3=\"0\" a4=\"(2 * pi)\"/>\n" +
			"	<output a0=\"curveFunction\"/>\n" +
			"</command>\n" +
			"<element type=\"curvecartesian\" label=\"curveFunction\">\n" +
			"	<show object=\"false\" label=\"false\" ev=\"4\"/>\n" +
			"	<objColor r=\"0\" g=\"0\" b=\"0\" alpha=\"0\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelOffset x=\"60\" y=\"-15\"/>\n" +
			"	<labelMode val=\"0\"/>\n" +
			"	<selectionAllowed val=\"false\"/>\n" +
			"	<lineStyle thickness=\"5\" type=\"0\" typeHidden=\"1\" opacity=\"178\"/>\n" +
			"</element>\n" +
			"<expression label=\"K\" exp=\"curveFunction(realAngle)\" type=\"point\" />\n" +
			"<element type=\"point\" label=\"K\">\n" +
			"	<show object=\"true\" label=\"true\" ev=\"4\"/>\n" +
			"	<objColor r=\"68\" g=\"68\" b=\"68\" alpha=\"0\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelMode val=\"0\"/>\n" +
			"	<pointSize val=\"4\"/>\n" +
			"	<pointStyle val=\"0\"/>\n" +
			"	<coords x=\"0.03953911977970617\" y=\"0.04778689686319611\" z=\"1\"/>\n" +
			"</element>\n" +
			"<command name=\"Vector\">\n" +
			"	<input a0=\"K\"/>\n" +
			"	<output a0=\"vSpace\"/>\n" +
			"</command>\n" +
			"<element type=\"vector\" label=\"vSpace\">\n" +
			"	<show object=\"false\" label=\"true\"/>\n" +
			"	<objColor r=\"0\" g=\"0\" b=\"0\" alpha=\"0\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelMode val=\"0\"/>\n" +
			"	<lineStyle thickness=\"5\" type=\"0\" typeHidden=\"1\" opacity=\"178\"/>\n" +
			"	<coordStyle style=\"cartesian\"/>\n" +
			"	<coords x=\"0.03953911977970617\" y=\"0.04778689686319611\" z=\"0\"/>\n" +
			"</element>\n" +
			"<expression label=\"allPoints\" exp=\"{A + vSpace, A - vSpace, B - vSpace, B + vSpace}\" />\n" +
			"<element type=\"list\" label=\"allPoints\">\n" +
			"	<show object=\"true\" label=\"true\" ev=\"4\"/>\n" +
			"	<objColor r=\"0\" g=\"100\" b=\"0\" alpha=\"0\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelMode val=\"0\"/>\n" +
			"	<lineStyle thickness=\"5\" type=\"0\" typeHidden=\"1\"/>\n" +
			"	<pointSize val=\"5\"/>\n" +
			"	<pointStyle val=\"0\"/>\n" +
			"	<angleStyle val=\"0\"/>\n" +
			"</element>\n" +
			"<command name=\"Polygon\">\n" +
			"	<input a0=\"allPoints\"/>\n" +
			"	<output a0=\"newPoly\"/>\n" +
			"</command>\n" +
			"<element type=\"polygon\" label=\"newPoly\">\n" +
			"	<lineStyle thickness=\"5\" type=\"0\" typeHidden=\"1\" opacity=\"178\"/>\n" +
			"	<show object=\"true\" label=\"false\"/>\n" +
			"	<objColor r=\"153\" g=\"51\" b=\"0\" alpha=\"0.10000000149011612\"/>\n" +
			"	<layer val=\"0\"/>\n" +
			"	<labelMode val=\"0\"/>\n" +
			"</element>\n" +
			"</construction>\n" +
			"</macro>\n" +
			"</geogebra>";

		// Convert XML to Gpad
		XMLToGpadConverter converter = new XMLToGpadConverter(null, xmlMacro, false);
		String gpad = converter.toGpad();
		
		assertNotNull("Gpad output should not be null", gpad);
		
		// Find the positions of pointGap and curveFunction in the output
		int pointGapIndex = gpad.indexOf("pointGap");
		int curveFunctionIndex = gpad.indexOf("curveFunction");
		
		// Both should be present
		assertTrue("pointGap should be present in output", pointGapIndex >= 0);
		assertTrue("curveFunction should be present in output", curveFunctionIndex >= 0);
		
		// pointGap should appear before curveFunction (due to dependency)
		// We need to find the actual definition lines, not just any occurrence
		// Look for "pointGap" followed by "=" and "curveFunction" followed by "*" or "="
		int pointGapDefIndex = gpad.indexOf("pointGap* =");
		
		int curveFunctionDefIndex = gpad.indexOf("curveFunction*");
		if (curveFunctionDefIndex < 0) {
			curveFunctionDefIndex = gpad.indexOf("curveFunction =");
		}
		if (curveFunctionDefIndex < 0) {
			curveFunctionDefIndex = gpad.indexOf("curveFunction=");
		}
		
		assertTrue("pointGap definition should be present", pointGapDefIndex >= 0);
		assertTrue("curveFunction definition should be present", curveFunctionDefIndex >= 0);
		
		// Verify dependency: pointGap should be defined before curveFunction
		assertTrue("pointGap should be defined before curveFunction (dependency order). " +
			"pointGap at " + pointGapDefIndex + ", curveFunction at " + curveFunctionDefIndex, 
			pointGapDefIndex < curveFunctionDefIndex);
	}
}
