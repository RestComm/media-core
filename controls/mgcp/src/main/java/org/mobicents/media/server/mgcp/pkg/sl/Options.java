/*
 * Telestax, Open Source Cloud Communications
 * Copyright 2013, Telestax, Inc. and individual contributors
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
package org.mobicents.media.server.mgcp.pkg.sl;

import java.util.concurrent.ArrayBlockingQueue;

import org.mobicents.media.server.concurrent.ConcurrentCyclicFIFO;
import org.mobicents.media.server.utils.Text;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author Yulian Oifa
 */
public final class Options {
	
	private static final ConcurrentCyclicFIFO<ArrayBlockingQueue<DtmfSignal>> cache = new ConcurrentCyclicFIFO<ArrayBlockingQueue<DtmfSignal>>();

	public static void recycle(ArrayBlockingQueue<DtmfSignal> queue) {
		queue.clear();
		cache.offer(queue);
	}

	public static ArrayBlockingQueue<DtmfSignal> parse(final Text data) {
		ArrayBlockingQueue<DtmfSignal> result = cache.poll();
		if (result == null) {
			result = new ArrayBlockingQueue<DtmfSignal>(100);
		}

		// current index and char, and total length
		int index = 0;
		char currChar;
		int length = data.length();

		// sub elements counter and flag
		boolean subToneEnded = false, numberEnded = false;
		int subElements = 0;

		// storage for sub elements
		String currentTone;
		int defaultDuration = 0;

		// at least 3 chars exists for tone d/ and something else either tone or
		// dd
		// so first 3 chars can be compared freely
		while (index < length - 2) {
			currentTone = null;
			defaultDuration = DtmfSignal.DEFAULT_DURATION;

			currChar = data.charAt(index++);
			if (currChar != 'd' && currChar != 'D') {
				recycle(result);
				throw new IllegalArgumentException();
			}

			currChar = data.charAt(index++);
			if (currChar != '/') {
				recycle(result);
				throw new IllegalArgumentException();
			}

			currChar = data.charAt(index++);
			if (currChar >= '0' && currChar <= '9') {
				currentTone = DtmfSignal.ALL_TONES[currChar - '0'];
			} else {
				switch (currChar) {
				case 'a':
				case 'A':
					currentTone = DtmfSignal.TONE_A;
					break;
				case 'b':
				case 'B':
					currentTone = DtmfSignal.TONE_B;
					break;
				case 'c':
				case 'C':
					currentTone = DtmfSignal.TONE_C;
					break;
				case '#':
					currentTone = DtmfSignal.TONE_HASH;
					break;
				case '*':
					currentTone = DtmfSignal.TONE_STAR;
					break;
				case 'd':
				case 'D':
					// may be simple d tone or dd
					if (index == length) {
						currentTone = DtmfSignal.TONE_D;
					} else if (index < length - 4) {
						// we have either ',' and next tone
						// or dd(something)
						// in first case we should have 3 chars for next tone
						// and ','
						// in second case we should have at least 4 chars , one
						// d , open and close bracket and something inside
						currChar = data.charAt(index);
						if (currChar == ',') {
							currentTone = DtmfSignal.TONE_D;
						} else if (currChar == 'd' || currChar == 'D') {
							if (data.charAt(++index) != '(') {
								recycle(result);
								throw new IllegalArgumentException();
							}

							subToneEnded = false;
							subElements = 0;
							index++;
							// here we should have at least 4 chars
							// 2 for dg or to , one for = , and at least one
							// more
							// we can have max 2 segment dg and to
							while (index < length - 4 && !subToneEnded && subElements < 2) {
								subElements++;
								currChar = data.charAt(index++);
								switch (currChar) {
								case 'd':
								case 'D':
									currChar = data.charAt(index++);
									if (currChar != 'g' && currChar != 'G') {
										recycle(result);
										throw new IllegalArgumentException();
									}

									currChar = data.charAt(index++);
									if (currChar != '=') {
										recycle(result);
										throw new IllegalArgumentException();
									}

									currChar = data.charAt(index++);
									if (currChar >= '0' && currChar <= '9') {
										currentTone = DtmfSignal.ALL_TONES[currChar - '0'];
									} else {
										switch (currChar) {
										case 'a':
										case 'A':
											currentTone = DtmfSignal.TONE_A;
											break;
										case 'b':
										case 'B':
											currentTone = DtmfSignal.TONE_B;
											break;
										case 'c':
										case 'C':
											currentTone = DtmfSignal.TONE_C;
											break;
										case 'd':
										case 'D':
											currentTone = DtmfSignal.TONE_D;
											break;
										case '#':
											currentTone = DtmfSignal.TONE_HASH;
											break;
										case '*':
											currentTone = DtmfSignal.TONE_STAR;
											break;
										}
									}

									currChar = data.charAt(index++);
									// either should be closed or should have
									// more segments
									if (currChar == ')') {
										subToneEnded = true;
									} else if (currChar != ',') {
										recycle(result);
										throw new IllegalArgumentException();
									}
									break;
								case 't':
								case 'T':
									currChar = data.charAt(index++);
									if (currChar != 'o' && currChar != 'O') {
										recycle(result);
										throw new IllegalArgumentException();
									}

									currChar = data.charAt(index++);
									if (currChar != '=') {
										recycle(result);
										throw new IllegalArgumentException();
									}

									// numeric value should come here
									numberEnded = false;
									defaultDuration = 0;
									while (!numberEnded && index < length) {
										currChar = data.charAt(index++);
										if (currChar >= '0' && currChar <= '9') {
											defaultDuration *= 10;
											defaultDuration += (currChar - '0');
										} else if (currChar == ')') {
											numberEnded = true;
											subToneEnded = true;
										} else if (currChar == ',')
											numberEnded = true;
										else {
											recycle(result);
											throw new IllegalArgumentException();
										}
									}

									if (!numberEnded) {
										// was number till the end
										recycle(result);
										throw new IllegalArgumentException();
									}

									break;
								}
							}

							if (!subToneEnded) {
								// either too much segments or too short last
								// one
								recycle(result);
								throw new IllegalArgumentException();
							}
						}
					} else {
						recycle(result);
						throw new IllegalArgumentException();
					}
					break;
				}
			}

			// here we have tone parsed , possibly have ',' char which means
			// have more
			// or simply end
			if (index < length) {
				currChar = data.charAt(index++);
				if (currChar != ',') {
					// some garbage left
					recycle(result);
					throw new IllegalArgumentException();
				}
			}

			result.offer(new DtmfSignal(currentTone, defaultDuration));
		}

		if (index != length) {
			// some garbage left
			recycle(result);
			throw new IllegalArgumentException();
		}

		return result;
	}
}