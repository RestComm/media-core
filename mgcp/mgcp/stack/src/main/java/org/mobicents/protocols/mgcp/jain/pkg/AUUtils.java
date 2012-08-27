/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.mgcp.jain.pkg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AUUtils {
	int index = 0;
	char[] chars = null;
	String rawString = null;
	int totalChars = -1;
	Value value = null;

	AUUtils(String rawString) {
		this.rawString = rawString;
		System.out.println("rawString = " + rawString);
		chars = rawString.toCharArray();
		totalChars = chars.length;
		value = new EventsValue();

		System.out.println("totalChars = " + totalChars);
	}

	public static void main(String args[]) throws ParserException {
		String s = "it=4 an=39<1212,ssss>,40[Lang=dan,gender=female,accent=cajun],47,ts(blaaa),dt(GOOOD),si(asssss)";

		AUUtils a = new AUUtils(s);
		Value v = a.decode_PlayAnnParmList();
		System.out.println("Here it come = " + v.get(ParameterEnum.an) + " " + v.get(ParameterEnum.it));

		s = "ip=21 rp=109 nd=102 fa=81 sa=72 dp=#*222 psk=9,nxt sik=*#12345667 na=3";
		a = new AUUtils(s);
		v = a.decode_PlayColParmList();
		System.out.println("Here it come = " + v.get(ParameterEnum.ip) + " " + v.get(ParameterEnum.rp) + " "
				+ v.get(ParameterEnum.nd) + " " + v.get(ParameterEnum.fa) + " " + v.get(ParameterEnum.sa) + " "
				+ v.get(ParameterEnum.na) + " " + v.get(ParameterEnum.dp) + " " + v.get(ParameterEnum.psk) + " "
				+ v.get(ParameterEnum.sik));
	}

	/*
	 * ABNF Definition
	 * 
	 * PlayAnnParmList = PlayAnnParm *( WSP PlayAnnParm ); PlayAnnParm = (
	 * AnnouncementParm / IterationsParm / IntervalParm / DurationParm /
	 * SpeedParm / VolumeParm ); AnnouncementParm = AnParmToken EQUALS
	 * Segmentlist; Segmentlist = SegmentDescriptor *( COMMA SegmentDescriptor );
	 * SegmentDescriptor = ( ( SegmentId [ EmbedVarList ] [ SegSelectorList ] ) / (
	 * TextToSpeechSeg [ SegSelectorList ] ) / ( DisplayTextSeg [
	 * SegSelectorList ] ) / ( VariableSeg [ SegSelectorList ] ) / SilenceSeg );
	 * 
	 * 
	 * IterationsParm = ItParmToken EQUALS ( NUMBER / "-1" ); IntervalParm =
	 * IvParmToken EQUALS NUMBER; DurationParm = DuParmToken EQUALS NUMBER;
	 * SpeedParm = SpParmToken EQUALS SIGNEDINT; VolumeParm = VlParmToken EQUALS
	 * SIGNEDINT;
	 */

	public Value decode_PlayAnnParmList() throws ParserException {
		boolean f = decode_PlayAnnParm();
		if (!f) {
			throw new ParserException("Parsing of AnnParm failed");
		}
		while (f) {
			f = decode_WSP();
			if (f) {
				f = decode_PlayAnnParm();
			}
		}

		return value;
	}

	public Value decode_PlayColParmList() throws ParserException {
		// PlayColParmList = PlayColParm *( WSP PlayColParm );
		boolean f = decode_PlayColParm();
		if (!f) {
			throw new ParserException("Parsing of PlayColParm failed");
		}
		while (f) {
			f = decode_WSP();
			if (f) {
				f = decode_PlayColParm();
			}
		}

		return value;
	}

	public Value decode_PlayRecParmList() throws ParserException {
		// PlayRecParmList = PlayRecParm *( WSP PlayRecParm );
		boolean f = decode_PlayRecParm();
		if (!f) {
			throw new ParserException("Parsing of PlayRecParm failed");
		}
		while (f) {
			f = decode_WSP();
			if (f) {
				f = decode_PlayRecParm();
			}
		}

		return value;
	}

	public Value decode_OpCompleteParmList() throws ParserException {
		// PlayRecParmList = PlayRecParm *( WSP PlayRecParm );
		boolean f = decode_OpCompleteParm();
		if (!f) {
			throw new ParserException("Parsing of OpCompleteParm failed");
		}
		while (f) {
			f = decode_WSP();
			if (f) {
				f = decode_OpCompleteParm();
			}
		}

		return value;
	}

	// Decode Space or HTAB
	private boolean decode_WSP() {
		boolean decoded = false;
		if (index < totalChars && (chars[index] == 0x20 || chars[index] == 0x09)) {
			index++;
			decoded = true;
		}
		return decoded;
	}

	private boolean decode_Segid(AnnouncementParmValue annPaVa) {
		boolean decoded = false;
		String strSegId = "";
		SegmentId segId = null;

		if (chars[index] >= '0' && chars[index] <= '9') {
			strSegId = strSegId + chars[index];
			decoded = true;
			index++;
		}
		if (decoded) {

			for (int i = 0; i <= 31; i++) {
				if (index < totalChars && (chars[index] >= '0' && chars[index] <= '9')) {
					strSegId = strSegId + chars[index];
					index++;
				} else {
					break;
				}
			}

			segId = new SegmentId(strSegId, null);
			annPaVa.addSegmentId(segId);
		} else if (chars[index] == '/') {
			index = index + 1;
			decoded = true;
			while (chars[index] != '/') {
				strSegId = strSegId + chars[index];
				index++;
			}
			segId = new SegmentId(null, strSegId);
			annPaVa.addSegmentId(segId);
		}

		if (decoded && index < totalChars) {

			if (chars[index] == '<') {
				index = index + 1;
				List<String> embedVarList = new ArrayList<String>();
				String tmp = "";
				while (chars[index] != '>') {
					tmp = tmp + chars[index];
					index = index + 1;
				}
				index = index + 1;
				String[] s = tmp.split(",");
				for (String sTemp : s) {
					embedVarList.add(sTemp);
				}
				segId.setEmbedVarList(embedVarList);
			}

			if (chars[index] == '[') {
				index = index + 1;
				Map<String, String> segSelectorMap = new HashMap<String, String>();
				String tmp = "";
				while (chars[index] != ']') {
					tmp = tmp + chars[index];
					index = index + 1;
				}
				index = index + 1;
				String[] s = tmp.split(",");
				for (String sTemp : s) {
					String[] s1 = sTemp.split("=");
					segSelectorMap.put(s1[0], s1[1]);

				}
				segId.setSegSelectorMap(segSelectorMap);
			}
		}
		return decoded;
	}

	private boolean decode_TextToSpeechSeg(AnnouncementParmValue annPaVa) {
		boolean decoded = false;
		String textTpSpeech = "";
		if (chars[index] == 't' && chars[index + 1] == 's') {
			index = index + 3;
			while (chars[index] != ')') {
				textTpSpeech = textTpSpeech + chars[index];
				index++;
			}
			decoded = true;
			index++;
			TextToSpeechSeg ts = new TextToSpeechSeg(textTpSpeech);
			annPaVa.addTextToSpeechSeg(ts);

			if (index < totalChars && chars[index] == '[') {
				index = index + 1;
				Map<String, String> segSelectorMap = new HashMap<String, String>();
				String tmp = "";
				while (chars[index] != ']') {
					tmp = tmp + chars[index];
					index = index + 1;
				}
				index = index + 1;
				String[] s = tmp.split(",");
				for (String sTemp : s) {
					String[] s1 = sTemp.split("=");
					segSelectorMap.put(s1[0], s1[1]);

				}
				ts.setSegSelectorMap(segSelectorMap);
			}
		}

		return decoded;
	}

	private boolean decode_DisplayTextSeg(AnnouncementParmValue annPaVa) {
		boolean decoded = false;
		String displayText = "";
		if (chars[index] == 'd' && chars[index + 1] == 't') {
			index = index + 3;
			while (chars[index] != ')') {
				displayText = displayText + chars[index];
				index++;
			}
			decoded = true;
			index++;
			DisplayTextSeg ds = new DisplayTextSeg(displayText);
			annPaVa.addDisplayTextSeg(ds);

			if (index < totalChars && chars[index] == '[') {
				index = index + 1;
				Map<String, String> segSelectorMap = new HashMap<String, String>();
				String tmp = "";
				while (chars[index] != ']') {
					tmp = tmp + chars[index];
					index = index + 1;
				}
				index = index + 1;
				String[] s = tmp.split(",");
				for (String sTemp : s) {
					String[] s1 = sTemp.split("=");
					segSelectorMap.put(s1[0], s1[1]);

				}
				ds.setSegSelectorMap(segSelectorMap);
			}
		}

		return decoded;
	}

	private boolean decode_SilenceSeg(AnnouncementParmValue annPaVa) {
		boolean decoded = false;
		if (index < totalChars && chars[index] == 's' && chars[index + 1] == 'i') {
			index = index + 3;
			String silenceSeg = "";
			while (chars[index] != ')') {
				silenceSeg = silenceSeg + chars[index];
				index++;
			}
			index++;

			SilenceSeg si = new SilenceSeg(silenceSeg);
			annPaVa.addSilenceSeg(si);
		}
		return decoded;
	}

	// SegmentDescriptor = ( ( SegmentId [ EmbedVarList ] [ SegSelectorList ] )
	// / ( TextToSpeechSeg [ SegSelectorList ] ) / ( DisplayTextSeg [
	// SegSelectorList ] ) / ( VariableSeg [ SegSelectorList ] ) / SilenceSeg );
	private boolean decode_SegmentDescriptor(AnnouncementParmValue annPaVa) throws ParserException {
		boolean decoded = false;
		decoded = decode_Segid(annPaVa);
		if (!decoded) {
			decoded = decode_TextToSpeechSeg(annPaVa);
		}
		if (!decoded) {
			decoded = decode_DisplayTextSeg(annPaVa);
		}
		// TODO VariableSeg impl pending

		if (!decoded) {
			decoded = decode_SilenceSeg(annPaVa);
		}
		return decoded;
	}

	// Segmentlist = SegmentDescriptor *( COMMA SegmentDescriptor );
	private boolean decode_Segmentlist(AnnouncementParmValue annPaVa) throws ParserException {
		boolean decoded = false;

		decoded = decode_SegmentDescriptor(annPaVa);
		if (decoded) {
			boolean f = true;

			while (f && index < totalChars) {
				if (chars[index] == ',') {
					index++;
					f = decode_SegmentDescriptor(annPaVa);
				} else {
					f = false;
				}

			}
		}
		return decoded;
	}

	/*
	 * OpCompleteParm = ( VoiceInterruptParm / IntKeySeqParm / NumAttemptsParm /
	 * AmtPlayedParm / DigitsColParm / RecordingIdParm / ReturnCodeParm );
	 */
	public boolean decode_OpCompleteParm() throws ParserException {
		boolean decoded = false;
		if (chars[index] == 'v' && chars[index + 1] == 'i') {
			// VoiceInterruptParm = ViParmToken EQUALS BOOLSTR;
			index = index + 3;
			boolean boolStrValue = decode_BOOLSTR();
			BooleanValue boolValue = new BooleanValue(ParameterEnum.vi, boolStrValue);
			value.put(ParameterEnum.vi, boolValue);
			decoded = true;
		} else if (chars[index] == 'i' && chars[index + 1] == 'k') {
			// IntKeySeqParm = IkParmToken EQUALS CommandKeySequence;
			index = index + 3;
			String cmdKeySequence = "";
			if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
				decoded = true;
				cmdKeySequence = cmdKeySequence + chars[index];
				index++;
				for (int i = 1; i < 3 && (index < totalChars); i++) {
					if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
						cmdKeySequence = cmdKeySequence + chars[index];
						index++;
					} else {
						break;
					}
				}

				StringValue s = new StringValue(ParameterEnum.ik, cmdKeySequence);
				value.put(ParameterEnum.ik, s);
				decoded = true;

			} else {
				throw new ParserException("Decoding of IntKeySeqParm failed");
			}

		} else if (chars[index] == 'n' && chars[index + 1] == 'a') {
			// NumAttemptsParm = NaParmToken EQUALS NUMBER;
			index = index + 3;
			int number = decode_NUMBER();
			NumberValue n = new NumberValue(ParameterEnum.na, number);
			value.put(ParameterEnum.na, n);
			decoded = true;
		} else if (chars[index] == 'a' && chars[index + 1] == 'p') {
			// AmtPlayedParm = ApParmToken EQUALS NUMBER;
			index = index + 3;
			int number = decode_NUMBER();
			NumberValue n = new NumberValue(ParameterEnum.ap, number);
			value.put(ParameterEnum.ap, n);
			decoded = true;
		} else if (chars[index] == 'd' && chars[index + 1] == 'c') {
			// DigitsColParm = DcParmToken EQUALS KeySequence;
			index = index + 3;
			String keySequence = "";
			if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
				decoded = true;
				keySequence = keySequence + chars[index];
				index++;
				for (int i = 1; i < 64 && (index < totalChars); i++) {
					if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
						keySequence = keySequence + chars[index];
						index++;
					} else {
						break;
					}
				}

				StringValue s = new StringValue(ParameterEnum.dc, keySequence);
				value.put(ParameterEnum.dc, s);
				decoded = true;

			} else {
				throw new ParserException("Decoding of DigitsColParm failed");
			}

		} else if (chars[index] == 'r' && chars[index + 1] == 'i') {
			// RecordingIdParm = RiParmToken EQUALS NUMBER;
			index = index + 3;
			int number = decode_NUMBER();
			NumberValue n = new NumberValue(ParameterEnum.ri, number);
			value.put(ParameterEnum.ri, n);
			decoded = true;

		} else if (chars[index] == 'r' && chars[index + 1] == 'c') {
			// ReturnCodeParm = RcParmToken EQUALS 3*3(DIGIT);
			index = index + 3;
			String rc = "";
			if (chars[index] >= '0' && chars[index] <= '9') {
				rc = rc + chars[index];
				index++;
				if (chars[index] >= '0' && chars[index] <= '9') {
					rc = rc + chars[index];
					index++;
					if (chars[index] >= '0' && chars[index] <= '9') {
						rc = rc + chars[index];
						index++;

						try {
							int number = Integer.parseInt(rc);
							NumberValue n = new NumberValue(ParameterEnum.rc, number);
							value.put(ParameterEnum.rc, n);
							decoded = true;
						} catch (NumberFormatException e) {
							throw new ParserException(
									"Decoding of ReturnCodeParm failed. The Return code is not number");
						}

					} else {
						throw new ParserException("Decoding of ReturnCodeParm failed");
					}
				} else {
					throw new ParserException("Decoding of ReturnCodeParm failed");
				}

			} else {
				throw new ParserException("Decoding of ReturnCodeParm failed");
			}

		} else {
			throw new ParserException("Decoding of PlayRecParm failed");
		}
		return decoded;
	}

	/*
	 * PlayRecParm = ( InitPromptParm / RepromptParm / NoSpeechParm /
	 * FailAnnParm / SuccessAnnParm / NoInterruptParm / SpeedParm / VolumeParm /
	 * ClearBufferParm / PreSpeechParm / PostSpeechParm / RecordLenParm /
	 * RestartKeyParm / ReinputKeyParm / ReturnKeyParm / PosKeyParm /
	 * StopKeyParm / EndInputKeyParm / RecPersistParm / OverrideAudioParm /
	 * RestoreAudioParm / DeletePersistParm / NumAttemptsParm );
	 */
	public boolean decode_PlayRecParm() throws ParserException {
		boolean decoded = false;
		if (chars[index] == 'i' && chars[index + 1] == 'p') {
			index = index + 3;
			AnnouncementParmValue annPaVa = new AnnouncementParmValue(ParameterEnum.ip);
			value.put(ParameterEnum.ip, annPaVa);
			// InitPromptParm = IpParmToken EQUALS Segmentlist;
			decoded = decode_Segmentlist(annPaVa);
		} else if (chars[index] == 'r' && chars[index + 1] == 'p') {
			index = index + 3;
			AnnouncementParmValue annPaVa = new AnnouncementParmValue(ParameterEnum.rp);
			value.put(ParameterEnum.rp, annPaVa);
			// RepromptParm = RpParmToken EQUALS Segmentlist;;
			decoded = decode_Segmentlist(annPaVa);
		} else if (chars[index] == 'n' && chars[index + 1] == 's') {
			// NoSpeechParm = NsParmToken EQUALS Segmentlist;
			index = index + 3;
			AnnouncementParmValue annPaVa = new AnnouncementParmValue(ParameterEnum.ns);
			value.put(ParameterEnum.ns, annPaVa);
			decoded = decode_Segmentlist(annPaVa);
		} else if (chars[index] == 'f' && chars[index + 1] == 'a') {
			index = index + 3;
			AnnouncementParmValue annPaVa = new AnnouncementParmValue(ParameterEnum.fa);
			value.put(ParameterEnum.fa, annPaVa);
			// FailAnnParm = FaParmToken EQUALS Segmentlist;
			decoded = decode_Segmentlist(annPaVa);
		} else if (chars[index] == 's' && chars[index + 1] == 'a') {
			index = index + 3;
			AnnouncementParmValue annPaVa = new AnnouncementParmValue(ParameterEnum.sa);
			value.put(ParameterEnum.sa, annPaVa);
			// SuccessAnnParm = SaParmToken EQUALS Segmentlist;
			decoded = decode_Segmentlist(annPaVa);
		} else if (chars[index] == 'n' && chars[index + 1] == 'i') {
			index = index + 3;
			// NoInterruptParm = NiParmToken EQUALS BOOLSTR;
			boolean boolStrValue = decode_BOOLSTR();
			BooleanValue boolValue = new BooleanValue(ParameterEnum.ni, boolStrValue);
			value.put(ParameterEnum.ni, boolValue);
			decoded = true;
		} else if (chars[index] == 's' && chars[index + 1] == 'p') {
			index = index + 3;
			// SpeedParm = SpParmToken EQUALS SIGNEDINT;
			String s = decode_SIGNEDINT();
			StringValue sValue = new StringValue(ParameterEnum.sp, s);
			value.put(ParameterEnum.sp, sValue);
			decoded = true;
		} else if (chars[index] == 'v' && chars[index + 1] == 'l') {
			index = index + 3;
			// VolumeParm = VlParmToken EQUALS SIGNEDINT;
			String s = decode_SIGNEDINT();
			StringValue sValue = new StringValue(ParameterEnum.vl, s);
			value.put(ParameterEnum.vl, sValue);
			decoded = true;
		} else if (chars[index] == 'c' && chars[index + 1] == 'b') {
			index = index + 3;
			// ClearBufferParm = CbParmToken EQUALS BOOLSTR;
			boolean boolStrValue = decode_BOOLSTR();
			BooleanValue boolValue = new BooleanValue(ParameterEnum.cb, boolStrValue);
			value.put(ParameterEnum.cb, boolValue);
			decoded = true;
		} else if (chars[index] == 'p' && chars[index + 1] == 'r' && chars[index + 2] == 't') {
			// PreSpeechParm = PrtParmToken EQUALS NUMBER;
			index = index + 4;
			int number = decode_NUMBER();
			NumberValue n = new NumberValue(ParameterEnum.prt, number);
			value.put(ParameterEnum.prt, n);
			decoded = true;

		} else if (chars[index] == 'p' && chars[index + 1] == 's' && chars[index + 2] == 't') {
			// PostSpeechParm = PstParmToken EQUALS NUMBER;
			index = index + 4;
			int number = decode_NUMBER();
			NumberValue n = new NumberValue(ParameterEnum.pst, number);
			value.put(ParameterEnum.pst, n);
			decoded = true;
		} else if (chars[index] == 'r' && chars[index + 1] == 'l' && chars[index + 2] == 't') {
			// RecordLenParm = RltParmToken EQUALS NUMBER;
			index = index + 4;
			int number = decode_NUMBER();
			NumberValue n = new NumberValue(ParameterEnum.rlt, number);
			value.put(ParameterEnum.rlt, n);
			decoded = true;
		} else if (chars[index] == 'r' && chars[index + 1] == 's' && chars[index + 2] == 'k') {
			// RestartKeyParm = RskParmToken EQUALS CommandKeySequence;
			index = index + 4;
			String cmdKeySequence = "";
			if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
				decoded = true;
				cmdKeySequence = cmdKeySequence + chars[index];
				index++;
				for (int i = 1; i < 3 && (index < totalChars); i++) {
					if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
						cmdKeySequence = cmdKeySequence + chars[index];
						index++;
					} else {
						break;
					}
				}

				StringValue s = new StringValue(ParameterEnum.rsk, cmdKeySequence);
				value.put(ParameterEnum.rsk, s);
				decoded = true;

			} else {
				throw new ParserException("Decoding of RestartKeyParm failed");
			}

		} else if (chars[index] == 'r' && chars[index + 1] == 'i' && chars[index + 2] == 'k') {
			// ReinputKeyParm = RikParmToken EQUALS CommandKeySequence;
			index = index + 4;
			String cmdKeySequence = "";
			if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
				decoded = true;
				cmdKeySequence = cmdKeySequence + chars[index];
				index++;
				for (int i = 1; i < 3 && (index < totalChars); i++) {
					if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
						cmdKeySequence = cmdKeySequence + chars[index];
						index++;
					} else {
						break;
					}
				}

				StringValue s = new StringValue(ParameterEnum.rik, cmdKeySequence);
				value.put(ParameterEnum.rik, s);
				decoded = true;
			} else {
				throw new ParserException("Decoding of ReinputKeyParm failed");
			}

		} else if (chars[index] == 'r' && chars[index + 1] == 't' && chars[index + 2] == 'k') {
			// ReturnKeyParm = RtkParmToken EQUALS CommandKeySequence;
			index = index + 4;
			String cmdKeySequence = "";
			if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
				decoded = true;
				cmdKeySequence = cmdKeySequence + chars[index];
				index++;
				for (int i = 1; i < 3 && (index < totalChars); i++) {
					if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
						cmdKeySequence = cmdKeySequence + chars[index];
						index++;
					} else {
						break;
					}
				}

				StringValue s = new StringValue(ParameterEnum.rtk, cmdKeySequence);
				value.put(ParameterEnum.rtk, s);
				decoded = true;
			} else {
				throw new ParserException("Decoding of ReinputKeyParm failed");
			}
		} else if (chars[index] == 'p' && chars[index + 1] == 's' && chars[index + 2] == 'k') {
			// PosKeyParm = PskParmToken EQUALS KeyPadKey COMMA
			// PosKeyAction;
			index = index + 4;

			if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
				String keyPadKey = String.valueOf(chars[index]);
				String posKeyAction = null;
				index++;
				if (chars[index] == ',') {
					index++;
					// PosKeyAction = FirstSegmentToken / LastSegmentToken /
					// PreviousSegmentToken / NextSegmentToken /
					// CurrentSegmentToken;
					if (chars[index] == 'f' && chars[index + 1] == 's' && chars[index + 2] == 't') {
						posKeyAction = "fst";
						index = index + 3;
					} else if (chars[index] == 'l' && chars[index + 1] == 's' && chars[index + 2] == 't') {
						posKeyAction = "lst";
						index = index + 3;
					} else if (chars[index] == 'p' && chars[index + 1] == 'r' && chars[index + 2] == 'v') {
						posKeyAction = "prv";
						index = index + 3;
					} else if (chars[index] == 'n' && chars[index + 1] == 'x' && chars[index + 2] == 't') {
						posKeyAction = "nxt";
						index = index + 3;
					} else if (chars[index] == 'c' && chars[index + 1] == 'u' && chars[index + 2] == 'r') {
						posKeyAction = "cur";
						index = index + 3;
					} else {
						throw new ParserException("Decoding of PosKeyParm's PosKeyAction failed");
					}
					PosKeyValue p = new PosKeyValue(ParameterEnum.psk, keyPadKey, posKeyAction);
					value.put(ParameterEnum.psk, p);
					decoded = true;
				} else {
					throw new ParserException("Decoding of PosKeyParm failed. No comma found after KeyPadKey");
				}

			} else {
				throw new ParserException("Decoding of PosKeyParm failed");
			}
		} else if (chars[index] == 's' && chars[index + 1] == 't' && chars[index + 2] == 'k') {
			// StopKeyParm = StkParmToken EQUALS KeyPadKey;
			index = index + 4;
			if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
				StringValue s = new StringValue(ParameterEnum.stk, String.valueOf(chars[index]));
				value.put(ParameterEnum.stk, s);
				index++;
				decoded = true;
			} else {
				throw new ParserException("Decoding of StopKeyParm failed.");
			}

		} else if (chars[index] == 'e' && chars[index + 1] == 'i' && chars[index + 2] == 'k') {
			// EndInputKeyParm = EikParmToken EQUALS KeyPadKey;
			index = index + 4;
			if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
				StringValue s = new StringValue(ParameterEnum.eik, String.valueOf(chars[index]));
				value.put(ParameterEnum.eik, s);
				index++;
				decoded = true;
			} else {
				throw new ParserException("Decoding of EndInputKeyParm failed.");
			}
		} else if (chars[index] == 'e' && chars[index + 1] == 'i' && chars[index + 2] == 'k') {
			// RecPersistParm = RpaParmToken EQUALS BOOLSTR;
			index = index + 4;
			boolean boolStrValue = decode_BOOLSTR();
			BooleanValue boolValue = new BooleanValue(ParameterEnum.eik, boolStrValue);
			value.put(ParameterEnum.eik, boolValue);
			decoded = true;
		} else if (chars[index] == 'o' && chars[index + 1] == 'a') {
			// OverrideAudioParm = OaParmToken EQUALS SEGID;
			int number = decode_NUMBER();
			NumberValue n = new NumberValue(ParameterEnum.oa, number);
			value.put(ParameterEnum.oa, n);
			decoded = true;
		} else if (chars[index] == 'r' && chars[index + 1] == 'a') {
			// RestoreAudioParm = RaParmToken EQUALS SEGID;
			int number = decode_NUMBER();
			NumberValue n = new NumberValue(ParameterEnum.ra, number);
			value.put(ParameterEnum.ra, n);
			decoded = true;
		} else if (chars[index] == 'd' && chars[index + 1] == 'p' && chars[index + 2] == 'a') {
			// DeletePersistParm = DpaParmToken EQUALS SEGID;
			int number = decode_NUMBER();
			NumberValue n = new NumberValue(ParameterEnum.dpa, number);
			value.put(ParameterEnum.dpa, n);
			decoded = true;
		} else if (chars[index] == 'n' && chars[index + 1] == 'a') {
			// NumAttemptsParm = NaParmToken EQUALS NUMBER;
			index = index + 3;
			int number = decode_NUMBER();
			NumberValue n = new NumberValue(ParameterEnum.na, number);
			value.put(ParameterEnum.na, n);
			decoded = true;
		} else {
			throw new ParserException("Decoding of PlayRecParm failed");
		}
		return decoded;
	}

	/*
	 * PlayColParm = ( InitPromptParm / RepromptParm / NoDigitsParm /
	 * FailAnnParm / SuccessAnnParm / NoInterruptParm / SpeedParm / VolumeParm /
	 * ClearBufferParm / MaxDigitsParm / MinDigitsParm / DigitPatternParm /
	 * FirstDigitParm / InterDigitParm / ExtraDigitParm / RestartKeyParm /
	 * ReinputKeyParm / ReturnKeyParm / PosKeyParm / StopKeyParm /
	 * StartInputKeyParm / EndInputKeyParm / IncludeEndInputKey /
	 * NumAttemptsParm );
	 */
	private boolean decode_PlayColParm() throws ParserException {
		boolean decoded = false;
		if (index < totalChars) {
			if (chars[index] == 'i' && chars[index + 1] == 'p') {
				index = index + 3;
				AnnouncementParmValue annPaVa = new AnnouncementParmValue(ParameterEnum.ip);
				value.put(ParameterEnum.ip, annPaVa);
				// InitPromptParm = IpParmToken EQUALS Segmentlist;
				decoded = decode_Segmentlist(annPaVa);
			} else if (chars[index] == 'r' && chars[index + 1] == 'p') {
				index = index + 3;
				AnnouncementParmValue annPaVa = new AnnouncementParmValue(ParameterEnum.rp);
				value.put(ParameterEnum.rp, annPaVa);
				// RepromptParm = RpParmToken EQUALS Segmentlist;;
				decoded = decode_Segmentlist(annPaVa);
			} else if (chars[index] == 'n' && chars[index + 1] == 'd') {
				index = index + 3;
				AnnouncementParmValue annPaVa = new AnnouncementParmValue(ParameterEnum.nd);
				value.put(ParameterEnum.nd, annPaVa);
				// NoDigitsParm = NdParmToken EQUALS Segmentlist;
				decoded = decode_Segmentlist(annPaVa);
			} else if (chars[index] == 'f' && chars[index + 1] == 'a') {
				index = index + 3;
				AnnouncementParmValue annPaVa = new AnnouncementParmValue(ParameterEnum.fa);
				value.put(ParameterEnum.fa, annPaVa);
				// FailAnnParm = FaParmToken EQUALS Segmentlist;
				decoded = decode_Segmentlist(annPaVa);
			} else if (chars[index] == 's' && chars[index + 1] == 'a') {
				index = index + 3;
				AnnouncementParmValue annPaVa = new AnnouncementParmValue(ParameterEnum.sa);
				value.put(ParameterEnum.sa, annPaVa);
				// SuccessAnnParm = SaParmToken EQUALS Segmentlist;
				decoded = decode_Segmentlist(annPaVa);
			} else if (chars[index] == 'n' && chars[index + 1] == 'i') {
				index = index + 3;
				// NoInterruptParm = NiParmToken EQUALS BOOLSTR;
				boolean boolStrValue = decode_BOOLSTR();
				BooleanValue boolValue = new BooleanValue(ParameterEnum.ni, boolStrValue);
				value.put(ParameterEnum.ni, boolValue);
				decoded = true;
			} else if (chars[index] == 's' && chars[index + 1] == 'p') {
				index = index + 3;
				// SpeedParm = SpParmToken EQUALS SIGNEDINT;
				String s = decode_SIGNEDINT();
				StringValue sValue = new StringValue(ParameterEnum.sp, s);
				value.put(ParameterEnum.sp, sValue);
				decoded = true;
			} else if (chars[index] == 'v' && chars[index + 1] == 'l') {
				index = index + 3;
				// VolumeParm = VlParmToken EQUALS SIGNEDINT;
				String s = decode_SIGNEDINT();
				StringValue sValue = new StringValue(ParameterEnum.vl, s);
				value.put(ParameterEnum.vl, sValue);
				decoded = true;
			} else if (chars[index] == 'c' && chars[index + 1] == 'b') {
				index = index + 3;
				// ClearBufferParm = CbParmToken EQUALS BOOLSTR;
				boolean boolStrValue = decode_BOOLSTR();
				BooleanValue boolValue = new BooleanValue(ParameterEnum.cb, boolStrValue);
				value.put(ParameterEnum.cb, boolValue);
				decoded = true;
			} else if (chars[index] == 'm' && chars[index + 1] == 'x') {
				index = index + 3;
				// MaxDigitsParm = MxParmToken EQUALS NUMBER;
				int number = decode_NUMBER();
				NumberValue n = new NumberValue(ParameterEnum.mx, number);
				value.put(ParameterEnum.mx, n);
				decoded = true;
			} else if (chars[index] == 'm' && chars[index + 1] == 'n') {
				// MinDigitsParm = MnParmToken EQUALS NUMBER;
				index = index + 3;
				int number = decode_NUMBER();
				NumberValue n = new NumberValue(ParameterEnum.mn, number);
				value.put(ParameterEnum.mn, n);
				decoded = true;
			} else if (chars[index] == 'd' && chars[index + 1] == 'p') {
				// DigitPatternParm = DpParmToken EQUALS DIGITPATTERN;
				// DIGITPATTERN = DigitString *(DigitString) ;
				// DigitString = DIGIT / "*" / "#" / "A" / "B" / "C" / "D";
				index = index + 3;
				String digitPattern = "";
				while (!(chars[index] == 0x20 || chars[index] == 0x09)) {
					digitPattern = digitPattern + chars[index];
					index++;
				}
				decoded = true;
				StringValue s = new StringValue(ParameterEnum.dp, digitPattern);
				value.put(ParameterEnum.dp, s);
			} else if (chars[index] == 'f' && chars[index + 1] == 'd' && chars[index + 2] == 't') {
				// FirstDigitParm = FdtParmToken EQUALS NUMBER;
				index = index + 4;
				int number = decode_NUMBER();
				NumberValue n = new NumberValue(ParameterEnum.fdt, number);
				value.put(ParameterEnum.fdt, n);
				decoded = true;

			} else if (chars[index] == 'i' && chars[index + 1] == 'd' && chars[index + 2] == 't') {
				// InterDigitParm = IdtParmToken EQUALS NUMBER;
				index = index + 4;
				int number = decode_NUMBER();
				NumberValue n = new NumberValue(ParameterEnum.idt, number);
				value.put(ParameterEnum.idt, n);
				decoded = true;

			} else if (chars[index] == 'e' && chars[index + 1] == 'd' && chars[index + 2] == 't') {
				// ExtraDigitParm = EdtParmToken EQUALS NUMBER;
				index = index + 4;
				int number = decode_NUMBER();
				NumberValue n = new NumberValue(ParameterEnum.edt, number);
				value.put(ParameterEnum.edt, n);
				decoded = true;

			} else if (chars[index] == 'r' && chars[index + 1] == 's' && chars[index + 2] == 'k') {
				// RestartKeyParm = RskParmToken EQUALS CommandKeySequence;
				index = index + 4;
				String cmdKeySequence = "";
				if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
					decoded = true;
					cmdKeySequence = cmdKeySequence + chars[index];
					index++;
					for (int i = 1; i < 3 && (index < totalChars); i++) {
						if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
							cmdKeySequence = cmdKeySequence + chars[index];
							index++;
						} else {
							break;
						}
					}

					StringValue s = new StringValue(ParameterEnum.rsk, cmdKeySequence);
					value.put(ParameterEnum.rsk, s);
					decoded = true;

				} else {
					throw new ParserException("Decoding of RestartKeyParm failed");
				}

			} else if (chars[index] == 'r' && chars[index + 1] == 'i' && chars[index + 2] == 'k') {
				// ReinputKeyParm = RikParmToken EQUALS CommandKeySequence;
				index = index + 4;
				String cmdKeySequence = "";
				if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
					decoded = true;
					cmdKeySequence = cmdKeySequence + chars[index];
					index++;
					for (int i = 1; i < 3 && (index < totalChars); i++) {
						if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
							cmdKeySequence = cmdKeySequence + chars[index];
							index++;
						} else {
							break;
						}
					}

					StringValue s = new StringValue(ParameterEnum.rik, cmdKeySequence);
					value.put(ParameterEnum.rik, s);
					decoded = true;
				} else {
					throw new ParserException("Decoding of ReinputKeyParm failed");
				}

			} else if (chars[index] == 'r' && chars[index + 1] == 't' && chars[index + 2] == 'k') {
				// ReturnKeyParm = RtkParmToken EQUALS CommandKeySequence;
				index = index + 4;
				String cmdKeySequence = "";
				if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
					decoded = true;
					cmdKeySequence = cmdKeySequence + chars[index];
					index++;
					for (int i = 1; i < 3 && (index < totalChars); i++) {
						if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
							cmdKeySequence = cmdKeySequence + chars[index];
							index++;
						} else {
							break;
						}
					}

					StringValue s = new StringValue(ParameterEnum.rtk, cmdKeySequence);
					value.put(ParameterEnum.rtk, s);
					decoded = true;
				} else {
					throw new ParserException("Decoding of ReinputKeyParm failed");
				}
			} else if (chars[index] == 'p' && chars[index + 1] == 's' && chars[index + 2] == 'k') {
				// PosKeyParm = PskParmToken EQUALS KeyPadKey COMMA
				// PosKeyAction;
				index = index + 4;

				if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
					String keyPadKey = String.valueOf(chars[index]);
					String posKeyAction = null;
					index++;
					if (chars[index] == ',') {
						index++;
						// PosKeyAction = FirstSegmentToken / LastSegmentToken /
						// PreviousSegmentToken / NextSegmentToken /
						// CurrentSegmentToken;
						if (chars[index] == 'f' && chars[index + 1] == 's' && chars[index + 2] == 't') {
							posKeyAction = "fst";
							index = index + 3;
						} else if (chars[index] == 'l' && chars[index + 1] == 's' && chars[index + 2] == 't') {
							posKeyAction = "lst";
							index = index + 3;
						} else if (chars[index] == 'p' && chars[index + 1] == 'r' && chars[index + 2] == 'v') {
							posKeyAction = "prv";
							index = index + 3;
						} else if (chars[index] == 'n' && chars[index + 1] == 'x' && chars[index + 2] == 't') {
							posKeyAction = "nxt";
							index = index + 3;
						} else if (chars[index] == 'c' && chars[index + 1] == 'u' && chars[index + 2] == 'r') {
							posKeyAction = "cur";
							index = index + 3;
						} else {
							throw new ParserException("Decoding of PosKeyParm's PosKeyAction failed");
						}
						PosKeyValue p = new PosKeyValue(ParameterEnum.psk, keyPadKey, posKeyAction);
						value.put(ParameterEnum.psk, p);
						decoded = true;
					} else {
						throw new ParserException("Decoding of PosKeyParm failed. No comma found after KeyPadKey");
					}

				} else {
					throw new ParserException("Decoding of PosKeyParm failed");
				}
			} else if (chars[index] == 's' && chars[index + 1] == 't' && chars[index + 2] == 'k') {
				// StopKeyParm = StkParmToken EQUALS KeyPadKey;
				index = index + 4;
				if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
					StringValue s = new StringValue(ParameterEnum.stk, String.valueOf(chars[index]));
					value.put(ParameterEnum.stk, s);
					index++;
					decoded = true;
				} else {
					throw new ParserException("Decoding of StopKeyParm failed.");
				}

			} else if (chars[index] == 's' && chars[index + 1] == 'i' && chars[index + 2] == 'k') {
				// StartInputKeyParm = SikParmToken EQUALS KeySet;
				index = index + 4;
				// KeySet = 1*11(KeyPadKey);
				String keySet = "";
				if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
					keySet = keySet + chars[index];
					index++;

					for (int i = 1; i < 11 && (!(chars[index] == 0x20 || chars[index] == 0x09)); i++) {
						keySet = keySet + chars[index];
						index++;
					}

					StringValue s = new StringValue(ParameterEnum.sik, keySet);
					value.put(ParameterEnum.sik, s);
					decoded = true;
				} else {
					throw new ParserException("Decoding of StartInputKeyParm failed.");
				}
			} else if (chars[index] == 'e' && chars[index + 1] == 'i' && chars[index + 2] == 'k') {
				// EndInputKeyParm = EikParmToken EQUALS KeyPadKey;
				index = index + 4;
				if ((chars[index] >= '0' && chars[index] <= '9') || chars[index] == '*' || chars[index] == '#') {
					StringValue s = new StringValue(ParameterEnum.eik, String.valueOf(chars[index]));
					value.put(ParameterEnum.eik, s);
					index++;
					decoded = true;
				} else {
					throw new ParserException("Decoding of EndInputKeyParm failed.");
				}

			} else if (chars[index] == 'i' && chars[index + 1] == 'e' && chars[index + 2] == 'k') {
				// IncludeEndinputKey = IekParmToken EQUALS BOOLSTR;
				index = index + 4;
				boolean boolStrValue = decode_BOOLSTR();
				BooleanValue boolValue = new BooleanValue(ParameterEnum.iek, boolStrValue);
				value.put(ParameterEnum.iek, boolValue);
				decoded = true;

			} else if (chars[index] == 'n' && chars[index + 1] == 'a') {
				// NumAttemptsParm = NaParmToken EQUALS NUMBER;
				index = index + 3;
				int number = decode_NUMBER();
				NumberValue n = new NumberValue(ParameterEnum.na, number);
				value.put(ParameterEnum.na, n);
				decoded = true;
			} else {
				throw new ParserException("Decoding of PlayColParm failed");
			}

		}
		return decoded;
	}

	private boolean decode_BOOLSTR() throws ParserException {
		boolean value = false;
		if (chars[index] == 't' && chars[index + 1] == 'r' && chars[index + 2] == 'u' && chars[index + 3] == 'e') {
			value = true;
			index = index + 5;
		} else if (chars[index] == 'f' && chars[index + 1] == 'a' && chars[index + 2] == 'l' && chars[index + 3] == 's'
				&& chars[index + 3] == 'e') {
			value = false;
			index = index + 6;
		} else {
			throw new ParserException("Parsing of BOOLSTR failed");
		}
		return value;
	}

	private String decode_SIGNEDINT() throws ParserException {
		String sign = "";

		if (chars[index] == '+') {
			sign = "+";
			index++;
		} else if (chars[index] == '-') {
			sign = "-";
			index++;
		} else {
			throw new ParserException("Parsing of SIGNEDINT failed");
		}

		return (sign + decode_NUMBER());
	}

	private int decode_NUMBER() throws ParserException {
		boolean decoded = false;
		String number = "";
		int num = 0;
		if (chars[index] >= '0' && chars[index] <= '9') {
			number = number + chars[index];
			index++;
			decoded = true;

			for (int i = 0; i < 31 && decoded && (index < totalChars); i++) {
				if (chars[index] >= '0' && chars[index] <= '9') {
					number = number + chars[index];
					index++;
					decoded = true;
				} else {
					decoded = false;
				}
			}

			try {
				num = Integer.parseInt(number);
			} catch (NumberFormatException e) {
				throw new ParserException("decode_NUMBER failed");
			}
		} else {
			throw new ParserException("decode_NUMBER failed");
		}
		return num;
	}

	/*
	 * PlayAnnParm = ( AnnouncementParm / IterationsParm / IntervalParm /
	 * DurationParm / SpeedParm / VolumeParm );
	 */
	private boolean decode_PlayAnnParm() throws ParserException {
		boolean decoded = false;
		if (index < totalChars) {
			if (chars[index] == 'a' && chars[index + 1] == 'n') {
				index = index + 3;
				AnnouncementParmValue annPaVa = new AnnouncementParmValue(ParameterEnum.an);
				value.put(ParameterEnum.an, annPaVa);

				decoded = decode_Segmentlist(annPaVa);
			} else if (chars[index] == 'i' && chars[index + 1] == 't') {

				index = index + 3;
				// IterationsParm = ItParmToken EQUALS ( NUMBER / "-1" );
				if (index < totalChars && chars[index] == '-') {
					NumberValue temp = new NumberValue(ParameterEnum.it, -1);
					value.put(ParameterEnum.it, temp);
					index = index + 3;
					decoded = true;
				} else {
					int interval = decode_NUMBER();
					NumberValue temp = new NumberValue(ParameterEnum.it, interval);
					value.put(ParameterEnum.it, temp);
					decoded = true;
				}

			} else if (chars[index] == 'i' && chars[index + 1] == 'v') {
				index = index + 3;
				// IntervalParm = IvParmToken EQUALS NUMBER;
				decoded = true;
				int interval = decode_NUMBER();
				NumberValue temp = new NumberValue(ParameterEnum.iv, interval);
				value.put(ParameterEnum.iv, temp);

			} else if (chars[index] == 'd' && chars[index + 1] == 'u') {
				index = index + 3;
				// DurationParm = DuParmToken EQUALS NUMBER;

				decoded = true;
				int interval = decode_NUMBER();
				NumberValue temp = new NumberValue(ParameterEnum.du, interval);
				value.put(ParameterEnum.du, temp);

			} else if (chars[index] == 's' && chars[index + 1] == 'p') {
				// SpeedParm = SpParmToken EQUALS SIGNEDINT;
				index = index + 3;
				String num = decode_SIGNEDINT();
				decoded = true;
				StringValue temp = new StringValue(ParameterEnum.sp, num);
				value.put(ParameterEnum.sp, temp);

			} else if (chars[index] == 'v' && chars[index + 1] == 'l') {
				// VolumeParm = VlParmToken EQUALS SIGNEDINT;
				index = index + 3;
				String num = decode_SIGNEDINT();
				decoded = true;
				StringValue temp = new StringValue(ParameterEnum.vl, num);
				value.put(ParameterEnum.vl, temp);
			} else {
				throw new ParserException(
						"PlayAnn decoding failed. None of AnnouncementParm / IterationsParm / IntervalParm / DurationParm / SpeedParm / VolumeParm found");
			}
		}

		return decoded;
	}
}
