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

package org.restcomm.media.codec.ilbc;

/**
 * 
 * @author oifa yulian 
 */
public class Constants {
	public static final short EPS=319;
	public static final short HALF_EPS=160;
	public static final short MAX_LSF=25723;
	public static final short MIN_LSF=82;
	  
	public static final short HP_IN_COEFICIENTS[] = {(short)3798,(short)-7596,(short)3798,(short)7807,(short)-3733};
	public static final short HP_OUT_COEFICIENTS[] = {(short)3849,(short)-7699,(short)3849,(short)7918,(short)-3833};
	
	public static final int LPC_LAG_WIN[]={2147483647,2144885453,2137754373,2125918626,2109459810,2088483140,2063130336,2033564590,1999977009,1962580174,1921610283};
	
	public static final short COS[] = {
	    (short)32767, (short)32729, (short)32610, (short)32413, (short)32138, (short)31786, (short)31357, (short)30853, (short)30274, (short)29622, (short)28899,
	    (short)28106, (short)27246, (short)26320, (short)25330, (short)24279, (short)23170, (short)22006, (short)20788, (short)19520, (short)18205, (short)16846,
	    (short)15447, (short)14010, (short)12540, (short)11039, (short)9512, (short)7962, (short)6393, (short)4808, (short)3212, (short)1608, (short)0, (short)-1608,
	    (short)-3212, (short)-4808, (short)-6393, (short)-7962, (short)-9512, (short)-11039, (short)-12540, (short)-14010, (short)-15447, (short)-16846, (short)-18205, 
	    (short)-19520, (short)-20788, (short)-22006, (short)-23170, (short)-24279, (short)-25330, (short)-26320, (short)-27246, (short)-28106, (short)-28899, (short)-29622,
	    (short)-30274, (short)-30853, (short)-31357, (short)-31786, (short)-32138, (short)-32413, (short)-32610, (short)-32729
	};
	
	public static final short COS_GRID[] = {
		(short)32760, (short)32723, (short)32588, (short)32364, (short)32051, (short)31651, (short)31164, (short)30591,(short)29935, (short)29196, (short)28377, 
		(short)27481, (short)26509, (short)25465, (short)24351, (short)23170,(short)21926, (short)20621, (short)19260, (short)17846, (short)16384, (short)14876, 
		(short)13327, (short)11743,(short)10125, (short)8480, (short)6812, (short)5126, (short)3425, (short)1714, (short)0, (short)-1714, (short)-3425,(short)-5126, 
		(short)-6812, (short)-8480, (short)-10125, (short)-11743, (short)-13327, (short)-14876,(short)-16384, (short)-17846, (short)-19260, (short)-20621, (short)-21926, 
		(short)-23170, (short)-24351,(short)-25465, (short)-26509, (short)-27481, (short)-28377, (short)-29196, (short)-29935, (short)-30591,(short)-31164, (short)-31651, 
		(short)-32051, (short)-32364, (short)-32588, (short)-32723, (short)-32760
	};
	
	public static final short COS_DERIVATIVE[] = {
		(short)-632, (short)-1893, (short)-3150, (short)-4399, (short)-5638, (short)-6863, (short)-8072, (short)-9261, (short)-10428, (short)-11570, (short)-12684, 
		(short)-13767, (short)-14817, (short)-15832, (short)-16808, (short)-17744, (short)-18637, (short)-19486, (short)-20287, (short)-21039, (short)-21741, 
		(short)-22390, (short)-22986, (short)-23526, (short)-24009, (short)-24435, (short)-24801, (short)-25108, (short)-25354, (short)-25540, (short)-25664, 
		(short)-25726, (short)-25726, (short)-25664, (short)-25540, (short)-25354, (short)-25108, (short)-24801, (short)-24435, (short)-24009, (short)-23526, 
		(short)-22986, (short)-22390, (short)-21741, (short)-21039, (short)-20287, (short)-19486, (short)-18637, (short)-17744, (short)-16808, (short)-15832, 
		(short)-14817, (short)-13767, (short)-12684, (short)-11570, (short)-10428, (short)-9261, (short)-8072, (short)-6863, (short)-5638, (short)-4399, (short)-3150,  
		(short)-1893,  (short)-632
	};
	
	public static final short ACOS_DERIVATIVE[] = {
		(short)-26887, (short)-8812, (short)-5323, (short)-3813, (short)-2979, (short)-2444, (short)-2081, (short)-1811, (short)-1608, (short)-1450, (short)-1322, 
		(short)-1219, (short)-1132, (short)-1059, (short)-998, (short)-946,(short)-901, (short)-861, (short)-827, (short)-797, (short)-772, (short)-750, (short)-730, 
		(short)-713,(short)-699, (short)-687, (short)-677, (short)-668, (short)-662, (short)-657, (short)-654, (short)-652, (short)-652, (short)-654, (short)-657, 
		(short)-662, (short)-668, (short)-677, (short)-687, (short)-699, (short)-713, (short)-730, (short)-750, (short)-772, (short)-797, (short)-827, (short)-861, 
		(short)-901,(short)-946, (short)-998, (short)-1059, (short)-1132, (short)-1219, (short)-1322, (short)-1450, (short)-1608, (short)-1811, (short)-2081, (short)-2444, 
		(short)-2979, (short)-3813, (short)-5323, (short)-8812, (short)-26887
	};
	
	public static final short LPC_WIN[] = {
		(short)6, (short)22, (short)50, (short)89, (short)139, (short)200, (short)272, (short)355, (short)449, (short)554, (short)669, (short)795, (short)932, 
		(short)1079, (short)1237, (short)1405, (short)1583, (short)1771, (short)1969, (short)2177, (short)2395, (short)2622, (short)2858, (short)3104, (short)3359, 
		(short)3622, (short)3894, (short)4175, (short)4464, (short)4761, (short)5066, (short)5379, (short)5699, (short)6026, (short)6361, (short)6702, (short)7050, 
		(short)7404, (short)7764, (short)8130, (short)8502, (short)8879, (short)9262, (short)9649, (short)10040, (short)10436, (short)10836, (short)11240, (short)11647, 
		(short)12058, (short)12471, (short)12887, (short)13306, (short)13726, (short)14148, (short)14572, (short)14997, (short)15423, (short)15850, (short)16277, (short)16704, 
		(short)17131, (short)17558, (short)17983, (short)18408, (short)18831, (short)19252, (short)19672, (short)20089, (short)20504, (short)20916, (short)21325, (short)21730, 
		(short)22132, (short)22530, (short)22924, (short)23314, (short)23698, (short)24078, (short)24452, (short)24821, (short)25185, (short)25542, (short)25893, (short)26238, 
		(short)26575, (short)26906, (short)27230, (short)27547, (short)27855, (short)28156, (short)28450, (short)28734, (short)29011, (short)29279, (short)29538, (short)29788, 
		(short)30029, (short)30261, (short)30483, (short)30696, (short)30899, (short)31092, (short)31275, (short)31448, (short)31611, (short)31764, (short)31906, (short)32037, 
		(short)32158, (short)32268, (short)32367, (short)32456, (short)32533, (short)32600, (short)32655, (short)32700, (short)32733, (short)32755, (short)32767, (short)32767, 
		(short)32755, (short)32733, (short)32700, (short)32655, (short)32600, (short)32533, (short)32456, (short)32367, (short)32268, (short)32158, (short)32037, (short)31906, 
		(short)31764, (short)31611, (short)31448, (short)31275, (short)31092, (short)30899, (short)30696, (short)30483, (short)30261, (short)30029, (short)29788, (short)29538, 
		(short)29279, (short)29011, (short)28734, (short)28450, (short)28156, (short)27855, (short)27547, (short)27230, (short)26906, (short)26575, (short)26238, (short)25893, 
		(short)25542, (short)25185, (short)24821, (short)24452, (short)24078, (short)23698, (short)23314, (short)22924, (short)22530, (short)22132, (short)21730, (short)21325, 
		(short)20916, (short)20504, (short)20089, (short)19672, (short)19252, (short)18831, (short)18408, (short)17983, (short)17558, (short)17131, (short)16704, (short)16277, 
		(short)15850, (short)15423, (short)14997, (short)14572, (short)14148, (short)13726, (short)13306, (short)12887, (short)12471, (short)12058, (short)11647, (short)11240, 
		(short)10836, (short)10436, (short)10040, (short)9649, (short)9262, (short)8879, (short)8502, (short)8130, (short)7764, (short)7404, (short)7050, (short)6702, (short)6361, 
		(short)6026, (short)5699, (short)5379, (short)5066, (short)4761, (short)4464, (short)4175, (short)3894, (short)3622, (short)3359, (short)3104, (short)2858, (short)2622, 
		(short)2395, (short)2177, (short)1969, (short)1771, (short)1583, (short)1405, (short)1237, (short)1079, (short)932, (short)795, (short)669, (short)554, (short)449, 
		(short)355, (short)272, (short)200, (short)139, (short)89, (short)50, (short)22, (short)6
	};
	
	public static final short LPC_ASYM_WIN[] = {
		(short)2, (short)7, (short)15, (short)27, (short)42, (short)60, (short)81, (short)106, (short)135, (short)166, (short)201, (short)239,
		(short)280, (short)325, (short)373, (short)424, (short)478, (short)536, (short)597, (short)661, (short)728, (short)798, (short)872, (short)949,
		(short)1028, (short)1111, (short)1197, (short)1287, (short)1379, (short)1474, (short)1572, (short)1674, (short)1778, (short)1885, (short)1995, (short)2108,
		(short)2224, (short)2343, (short)2465, (short)2589, (short)2717, (short)2847, (short)2980, (short)3115, (short)3254, (short)3395, (short)3538, (short)3684,
		(short)3833, (short)3984, (short)4138, (short)4295, (short)4453, (short)4615, (short)4778, (short)4944, (short)5112, (short)5283, (short)5456, (short)5631,
		(short)5808, (short)5987, (short)6169, (short)6352, (short)6538, (short)6725, (short)6915, (short)7106, (short)7300, (short)7495, (short)7692, (short)7891,
		(short)8091, (short)8293, (short)8497, (short)8702, (short)8909, (short)9118, (short)9328, (short)9539, (short)9752, (short)9966, (short)10182, (short)10398,
		(short)10616, (short)10835, (short)11055, (short)11277, (short)11499, (short)11722, (short)11947, (short)12172, (short)12398, (short)12625, (short)12852, (short)13080,
		(short)13309, (short)13539, (short)13769, (short)14000, (short)14231, (short)14463, (short)14695, (short)14927, (short)15160, (short)15393, (short)15626, (short)15859,
		(short)16092, (short)16326, (short)16559, (short)16792, (short)17026, (short)17259, (short)17492, (short)17725, (short)17957, (short)18189, (short)18421, (short)18653,
		(short)18884, (short)19114, (short)19344, (short)19573, (short)19802, (short)20030, (short)20257, (short)20483, (short)20709, (short)20934, (short)21157, (short)21380,
		(short)21602, (short)21823, (short)22042, (short)22261, (short)22478, (short)22694, (short)22909, (short)23123, (short)23335, (short)23545, (short)23755, (short)23962,
		(short)24168, (short)24373, (short)24576, (short)24777, (short)24977, (short)25175, (short)25371, (short)25565, (short)25758, (short)25948, (short)26137, (short)26323,
		(short)26508, (short)26690, (short)26871, (short)27049, (short)27225, (short)27399, (short)27571, (short)27740, (short)27907, (short)28072, (short)28234, (short)28394,
		(short)28552, (short)28707, (short)28860, (short)29010, (short)29157, (short)29302, (short)29444, (short)29584, (short)29721, (short)29855, (short)29987, (short)30115,
		(short)30241, (short)30364, (short)30485, (short)30602, (short)30717, (short)30828, (short)30937, (short)31043, (short)31145, (short)31245, (short)31342, (short)31436,
		(short)31526, (short)31614, (short)31699, (short)31780, (short)31858, (short)31933, (short)32005, (short)32074, (short)32140, (short)32202, (short)32261, (short)32317,
		(short)32370, (short)32420, (short)32466, (short)32509, (short)32549, (short)32585, (short)32618, (short)32648, (short)32675, (short)32698, (short)32718, (short)32734,
		(short)32748, (short)32758, (short)32764, (short)32767, (short)32767, (short)32667, (short)32365, (short)31863, (short)31164, (short)30274, (short)29197, (short)27939,
		(short)26510, (short)24917, (short)23170, (short)21281, (short)19261, (short)17121, (short)14876, (short)12540, (short)10126, (short)7650, (short)5126, (short)2571
	};
	
	public static final short LPC_CHIRP_WEIGHT_DENUM[] = {
		(short)32767, (short)13835, (short)5841, (short)2466, (short)1041, (short)440, (short)186, (short)78,  (short)33,  (short)14,  (short)6
	};
	
	public static final short LPC_CHIRP_SYNT_DENUM[] = {
		(short)32767, (short)29573, (short)26690, (short)24087,(short)21739, (short)19619, (short)17707, (short)15980,(short)14422, (short)13016, (short)11747};
		
	public static final short LSP_MEAN[] = {
		(short)31476, (short)29565, (short)25819, (short)18725, (short)10276,(short)1236, (short)-9049, (short)-17600, (short)-25884, (short)-30618};
	
	public static final short LSF_MEAN[] = {
		(short)2308, (short)3652, (short)5434, (short)7885,(short)10255, (short)12559, (short)15160, (short)17513,(short)20328, (short)22752};
	
	public static final short LSF_WEIGHT_20MS[] = { (short)12288, (short)8192, (short)4096, (short)0 };
	public static final short LSF_WEIGHT_30MS[] = { (short)8192, (short)16384, (short)10923, (short)5461, (short)0, (short)0 };
	
	public static final short LSF_CB[] = {
		(short)1273, (short)2238, (short)3696, 
		(short)3199, (short)5309, (short)8209, 
		(short)3606, (short)5671, (short)7829, 
		(short)2815, (short)5262, (short)8778,
		(short)2608, (short)4027, (short)5493, 
		(short)1582, (short)3076, (short)5945, 
		(short)2983, (short)4181, (short)5396, 
		(short)2437, (short)4322, (short)6902,
		(short)1861, (short)2998, (short)4613, 
		(short)2007, (short)3250, (short)5214, 
		(short)1388, (short)2459, (short)4262, 
		(short)2563, (short)3805, (short)5269,
		(short)2036, (short)3522, (short)5129, 
		(short)1935, (short)4025, (short)6694, 
		(short)2744, (short)5121, (short)7338, 
		(short)2810, (short)4248, (short)5723,
		(short)3054, (short)5405, (short)7745, 
		(short)1449, (short)2593, (short)4763, 
		(short)3411, (short)5128, (short)6596, 
		(short)2484, (short)4659, (short)7496,
		(short)1668, (short)2879, (short)4818, 
		(short)1812, (short)3072, (short)5036, 
		(short)1638, (short)2649, (short)3900, 
		(short)2464, (short)3550, (short)4644,
		(short)1853, (short)2900, (short)4158, 
		(short)2458, (short)4163, (short)5830, 
		(short)2556, (short)4036, (short)6254, 
		(short)2703, (short)4432, (short)6519,
		(short)3062, (short)4953, (short)7609, 
		(short)1725, (short)3703, (short)6187, 
		(short)2221, (short)3877, (short)5427, 
		(short)2339, (short)3579, (short)5197,
		(short)2021, (short)4633, (short)7037, 
		(short)2216, (short)3328, (short)4535, 
		(short)2961, (short)4739, (short)6667, 
		(short)2807, (short)3955, (short)5099,
		(short)2788, (short)4501, (short)6088, 
		(short)1642, (short)2755, (short)4431, 
		(short)3341, (short)5282, (short)7333, 
		(short)2414, (short)3726, (short)5727,
		(short)1582, (short)2822, (short)5269, 
		(short)2259, (short)3447, (short)4905, 
		(short)3117, (short)4986, (short)7054, 
		(short)1825, (short)3491, (short)5542,
		(short)3338, (short)5736, (short)8627, 
		(short)1789, (short)3090, (short)5488, 
		(short)2566, (short)3720, (short)4923, 
		(short)2846, (short)4682, (short)7161,
		(short)1950, (short)3321, (short)5976, 
		(short)1834, (short)3383, (short)6734, 
		(short)3238, (short)4769, (short)6094, 
		(short)2031, (short)3978, (short)5903,
		(short)1877, (short)4068, (short)7436, 
		(short)2131, (short)4644, (short)8296, 
		(short)2764, (short)5010, (short)8013, 
		(short)2194, (short)3667, (short)6302,
		(short)2053, (short)3127, (short)4342, 
		(short)3523, (short)6595, (short)10010,
		(short)3134, (short)4457, (short)5748, 
		(short)3142, (short)5819, (short)9414,
		(short)2223, (short)4334, (short)6353, 
		(short)2022, (short)3224, (short)4822, 
		(short)2186, (short)3458, (short)5544, 
		(short)2552, (short)4757, (short)6870,
		
		(short)10905,(short)12917,(short)14578,
		(short)9503, (short)11485,(short)14485,
		(short)9518, (short)12494,(short)14052,
		(short)6222, (short)7487, (short)9174,
		(short)7759, (short)9186, (short)10506,
		(short)8315, (short)12755,(short)14786,
		(short)9609, (short)11486,(short)13866,
		(short)8909, (short)12077,(short)13643,
		(short)7369, (short)9054, (short)11520,
		(short)9408, (short)12163,(short)14715,
		(short)6436, (short)9911, (short)12843,
		(short)7109, (short)9556, (short)11884,
		(short)7557, (short)10075,(short)11640,
		(short)6482, (short)9202, (short)11547,
		(short)6463, (short)7914, (short)10980,
		(short)8611, (short)10427,(short)12752,
		(short)7101, (short)9676, (short)12606,
		(short)7428, (short)11252,(short)13172,
		(short)10197,(short)12955,(short)15842,
		(short)7487, (short)10955,(short)12613,
		(short)5575, (short)7858, (short)13621,
		(short)7268, (short)11719,(short)14752,
		(short)7476, (short)11744,(short)13795,
		(short)7049, (short)8686, (short)11922,
		(short)8234, (short)11314,(short)13983,
		(short)6560, (short)11173,(short)14984,
		(short)6405, (short)9211, (short)12337,
		(short)8222, (short)12054,(short)13801,
		(short)8039, (short)10728,(short)13255,
		(short)10066,(short)12733,(short)14389,
		(short)6016, (short)7338, (short)10040,
		(short)6896, (short)8648, (short)10234,
		(short)7538, (short)9170, (short)12175,
		(short)7327, (short)12608,(short)14983,
		(short)10516,(short)12643,(short)15223,
		(short)5538, (short)7644, (short)12213,
		(short)6728, (short)12221,(short)14253,
		(short)7563, (short)9377, (short)12948,
		(short)8661, (short)11023,(short)13401,
		(short)7280, (short)8806, (short)11085,
		(short)7723, (short)9793, (short)12333,
		(short)12225,(short)14648,(short)16709,
		(short)8768, (short)13389,(short)15245,
		(short)10267,(short)12197,(short)13812,
		(short)5301, (short)7078, (short)11484,
		(short)7100, (short)10280,(short)11906,
		(short)8716, (short)12555,(short)14183,
		(short)9567, (short)12464,(short)15434,
		(short)7832, (short)12305,(short)14300,
		(short)7608, (short)10556,(short)12121,
		(short)8913, (short)11311,(short)12868,
		(short)7414, (short)9722, (short)11239,
		(short)8666, (short)11641,(short)13250,
		(short)9079, (short)10752,(short)12300,
		(short)8024, (short)11608,(short)13306,
		(short)10453,(short)13607,(short)16449,
		(short)8135, (short)9573, (short)10909,
		(short)6375, (short)7741, (short)10125,
		(short)10025,(short)12217,(short)14874,
		(short)6985, (short)11063,(short)14109,
		(short)9296, (short)13051,(short)14642,
		(short)8613, (short)10975,(short)12542,
		(short)6583, (short)10414,(short)13534,
		(short)6191, (short)9368, (short)13430,
		(short)5742, (short)6859, (short)9260, 
		(short)7723, (short)9813, (short)13679,
		(short)8137, (short)11291,(short)12833,
		(short)6562, (short)8973, (short)10641,
		(short)6062, (short)8462, (short)11335,
		(short)6928, (short)8784, (short)12647,
		(short)7501, (short)8784, (short)10031,
		(short)8372, (short)10045,(short)12135,
		(short)8191, (short)9864, (short)12746,
		(short)5917, (short)7487, (short)10979,
		(short)5516, (short)6848, (short)10318,
		(short)6819, (short)9899, (short)11421,
		(short)7882, (short)12912,(short)15670,
		(short)9558, (short)11230,(short)12753,
		(short)7752, (short)9327, (short)11472,
		(short)8479, (short)9980, (short)11358,
		(short)11418,(short)14072,(short)16386,
		(short)7968, (short)10330,(short)14423,
		(short)8423, (short)10555,(short)12162,
		(short)6337, (short)10306,(short)14391,
		(short)8850, (short)10879,(short)14276,
		(short)6750, (short)11885,(short)15710,
		(short)7037, (short)8328, (short)9764, 
		(short)6914, (short)9266, (short)13476,
		(short)9746, (short)13949,(short)15519,
		(short)11032,(short)14444,(short)16925,
		(short)8032, (short)10271,(short)11810,
		(short)10962,(short)13451,(short)15833,
		(short)10021,(short)11667,(short)13324,
		(short)6273, (short)8226, (short)12936,
		(short)8543, (short)10397,(short)13496,
		(short)7936, (short)10302,(short)12745,
		(short)6769, (short)8138, (short)10446,
		(short)6081, (short)7786, (short)11719,
		(short)8637, (short)11795,(short)14975,
		(short)8790, (short)10336,(short)11812,
		(short)7040, (short)8490, (short)10771,
		(short)7338, (short)10381,(short)13153,
		(short)6598, (short)7888, (short)9358, 
		(short)6518, (short)8237, (short)12030,
		(short)9055, (short)10763,(short)12983,
		(short)6490, (short)10009,(short)12007,
		(short)9589, (short)12023,(short)13632,
		(short)6867, (short)9447, (short)10995,
		(short)7930, (short)9816, (short)11397,
		(short)10241,(short)13300,(short)14939,
		(short)5830, (short)8670, (short)12387,
		(short)9870, (short)11915,(short)14247,
		(short)9318, (short)11647,(short)13272,
		(short)6721, (short)10836,(short)12929,
		(short)6543, (short)8233, (short)9944, 
		(short)8034, (short)10854,(short)12394,
		(short)9112, (short)11787,(short)14218,
		(short)9302, (short)11114,(short)13400,
		(short)9022, (short)11366,(short)13816,
		(short)6962, (short)10461,(short)12480,
		(short)11288,(short)13333,(short)15222,
		(short)7249, (short)8974, (short)10547,
		(short)10566,(short)12336,(short)14390,
		(short)6697, (short)11339,(short)13521,
		(short)11851,(short)13944,(short)15826,
		(short)6847, (short)8381, (short)11349,
		(short)7509, (short)9331, (short)10939,
		(short)8029, (short)9618, (short)11909,
		
		(short)13973,(short)17644,(short)19647,(short)22474,
		(short)14722,(short)16522,(short)20035,(short)22134,
		(short)16305,(short)18179,(short)21106,(short)23048,
		(short)15150,(short)17948,(short)21394,(short)23225,
		(short)13582,(short)15191,(short)17687,(short)22333,
		(short)11778,(short)15546,(short)18458,(short)21753,
		(short)16619,(short)18410,(short)20827,(short)23559,
		(short)14229,(short)15746,(short)17907,(short)22474,
		(short)12465,(short)15327,(short)20700,(short)22831,
		(short)15085,(short)16799,(short)20182,(short)23410,
		(short)13026,(short)16935,(short)19890,(short)22892,
		(short)14310,(short)16854,(short)19007,(short)22944,
		(short)14210,(short)15897,(short)18891,(short)23154,
		(short)14633,(short)18059,(short)20132,(short)22899,
		(short)15246,(short)17781,(short)19780,(short)22640,
		(short)16396,(short)18904,(short)20912,(short)23035,
		(short)14618,(short)17401,(short)19510,(short)21672,
		(short)15473,(short)17497,(short)19813,(short)23439,
		(short)18851,(short)20736,(short)22323,(short)23864,
		(short)15055,(short)16804,(short)18530,(short)20916,
		(short)16490,(short)18196,(short)19990,(short)21939,
		(short)11711,(short)15223,(short)21154,(short)23312,
		(short)13294,(short)15546,(short)19393,(short)21472,
		(short)12956,(short)16060,(short)20610,(short)22417,
		(short)11628,(short)15843,(short)19617,(short)22501,
		(short)14106,(short)16872,(short)19839,(short)22689,
		(short)15655,(short)18192,(short)20161,(short)22452,
		(short)12953,(short)15244,(short)20619,(short)23549,
		(short)15322,(short)17193,(short)19926,(short)21762,
		(short)16873,(short)18676,(short)20444,(short)22359,
		(short)14874,(short)17871,(short)20083,(short)21959,
		(short)11534,(short)14486,(short)19194,(short)21857,
		(short)17766,(short)19617,(short)21338,(short)23178,
		(short)13404,(short)15284,(short)19080,(short)23136,
		(short)15392,(short)17527,(short)19470,(short)21953,
		(short)14462,(short)16153,(short)17985,(short)21192,
		(short)17734,(short)19750,(short)21903,(short)23783,
		(short)16973,(short)19096,(short)21675,(short)23815,
		(short)16597,(short)18936,(short)21257,(short)23461,
		(short)15966,(short)17865,(short)20602,(short)22920,
		(short)15416,(short)17456,(short)20301,(short)22972,
		(short)18335,(short)20093,(short)21732,(short)23497,
		(short)15548,(short)17217,(short)20679,(short)23594,
		(short)15208,(short)16995,(short)20816,(short)22870,
		(short)13890,(short)18015,(short)20531,(short)22468,
		(short)13211,(short)15377,(short)19951,(short)22388,
		(short)12852,(short)14635,(short)17978,(short)22680,
		(short)16002,(short)17732,(short)20373,(short)23544,
		(short)11373,(short)14134,(short)19534,(short)22707,
		(short)17329,(short)19151,(short)21241,(short)23462,
		(short)15612,(short)17296,(short)19362,(short)22850,
		(short)15422,(short)19104,(short)21285,(short)23164,
		(short)13792,(short)17111,(short)19349,(short)21370,
		(short)15352,(short)17876,(short)20776,(short)22667,
		(short)15253,(short)16961,(short)18921,(short)22123,
		(short)14108,(short)17264,(short)20294,(short)23246,
		(short)15785,(short)17897,(short)20010,(short)21822,
		(short)17399,(short)19147,(short)20915,(short)22753,
		(short)13010,(short)15659,(short)18127,(short)20840,
		(short)16826,(short)19422,(short)22218,(short)24084,
		(short)18108,(short)20641,(short)22695,(short)24237,
		(short)18018,(short)20273,(short)22268,(short)23920,
		(short)16057,(short)17821,(short)21365,(short)23665,
		(short)16005,(short)17901,(short)19892,(short)23016,
		(short)13232,(short)16683,(short)21107,(short)23221,
		(short)13280,(short)16615,(short)19915,(short)21829,
		(short)14950,(short)18575,(short)20599,(short)22511,
		(short)16337,(short)18261,(short)20277,(short)23216,
		(short)14306,(short)16477,(short)21203,(short)23158,
		(short)12803,(short)17498,(short)20248,(short)22014,
		(short)14327,(short)17068,(short)20160,(short)22006,
		(short)14402,(short)17461,(short)21599,(short)23688,
		(short)16968,(short)18834,(short)20896,(short)23055,
		(short)15070,(short)17157,(short)20451,(short)22315,
		(short)15419,(short)17107,(short)21601,(short)23946,
		(short)16039,(short)17639,(short)19533,(short)21424,
		(short)16326,(short)19261,(short)21745,(short)23673,
		(short)16489,(short)18534,(short)21658,(short)23782,
		(short)16594,(short)18471,(short)20549,(short)22807,
		(short)18973,(short)21212,(short)22890,(short)24278,
		(short)14264,(short)18674,(short)21123,(short)23071,
		(short)15117,(short)16841,(short)19239,(short)23118,
		(short)13762,(short)15782,(short)20478,(short)23230,
		(short)14111,(short)15949,(short)20058,(short)22354,
		(short)14990,(short)16738,(short)21139,(short)23492,
		(short)13735,(short)16971,(short)19026,(short)22158,
		(short)14676,(short)17314,(short)20232,(short)22807,
		(short)16196,(short)18146,(short)20459,(short)22339,
		(short)14747,(short)17258,(short)19315,(short)22437,
		(short)14973,(short)17778,(short)20692,(short)23367,
		(short)15715,(short)17472,(short)20385,(short)22349,
		(short)15702,(short)18228,(short)20829,(short)23410,
		(short)14428,(short)16188,(short)20541,(short)23630,
		(short)16824,(short)19394,(short)21365,(short)23246,
		(short)13069,(short)16392,(short)18900,(short)21121,
		(short)12047,(short)16640,(short)19463,(short)21689,
		(short)14757,(short)17433,(short)19659,(short)23125,
		(short)15185,(short)16930,(short)19900,(short)22540,
		(short)16026,(short)17725,(short)19618,(short)22399,
		(short)16086,(short)18643,(short)21179,(short)23472,
		(short)15462,(short)17248,(short)19102,(short)21196,
		(short)17368,(short)20016,(short)22396,(short)24096,
		(short)12340,(short)14475,(short)19665,(short)23362,
		(short)13636,(short)16229,(short)19462,(short)22728,
		(short)14096,(short)16211,(short)19591,(short)21635,
		(short)12152,(short)14867,(short)19943,(short)22301,
		(short)14492,(short)17503,(short)21002,(short)22728,
		(short)14834,(short)16788,(short)19447,(short)21411,
		(short)14650,(short)16433,(short)19326,(short)22308,
		(short)14624,(short)16328,(short)19659,(short)23204,
		(short)13888,(short)16572,(short)20665,(short)22488,
		(short)12977,(short)16102,(short)18841,(short)22246,
		(short)15523,(short)18431,(short)21757,(short)23738,
		(short)14095,(short)16349,(short)18837,(short)20947,
		(short)13266,(short)17809,(short)21088,(short)22839,
		(short)15427,(short)18190,(short)20270,(short)23143,
		(short)11859,(short)16753,(short)20935,(short)22486,
		(short)12310,(short)17667,(short)21736,(short)23319,
		(short)14021,(short)15926,(short)18702,(short)22002,
		(short)12286,(short)15299,(short)19178,(short)21126,
		(short)15703,(short)17491,(short)21039,(short)23151,
		(short)12272,(short)14018,(short)18213,(short)22570,
		(short)14817,(short)16364,(short)18485,(short)22598,
		(short)17109,(short)19683,(short)21851,(short)23677,
		(short)12657,(short)14903,(short)19039,(short)22061,
		(short)14713,(short)16487,(short)20527,(short)22814,
		(short)14635,(short)16726,(short)18763,(short)21715,
		(short)15878,(short)18550,(short)20718,(short)22906
	};

	public static final short SCALE[] = {
		/* Values in Q16 */
		(short)29485, (short)25003, (short)21345, (short)18316, (short)15578, (short)13128, (short)10973, (short)9310, (short)7955, (short)6762, (short)5789, (short)4877, 
		(short)4255, (short)3699, (short)3258, (short)2904, (short)2595, (short)2328, (short)2123, (short)1932, (short)1785, (short)1631, (short)1493, 
		(short)1370, (short)1260, (short)1167, (short)1083,
		/* Values in Q21 */
		(short)32081, (short)29611, (short)27262, (short)25229, (short)23432, (short)21803, (short)20226, (short)18883, (short)17609, (short)16408, (short)15311, (short)14327, 
		(short)13390, (short)12513, (short)11693, (short)10919, (short)10163, (short)9435, (short)8739, (short)8100, (short)7424, (short)6813, (short)6192, (short)5648, (short)5122, 
		(short)4639, (short)4207, (short)3798, (short)3404, (short)3048, (short)2706, (short)2348, (short)2036, (short)1713, (short)1393, (short)1087, (short)747
	};
	
	public static final short STATE_SQ3[] = { /* Values in Q13 */
		(short)-30473, (short)-17838, (short)-9257, (short)-2537,(short)3639, (short)10893, (short)19958, (short)32636
	};
	
	public static final short FRQ_QUANT_MOD[] = {
		/* First 37 values in Q8 */
		(short)569, (short)671, (short)786, (short)916, (short)1077, (short)1278, (short)1529, (short)1802, (short)2109, (short)2481, (short)2898, (short)3440, (short)3943, 
		(short)4535, (short)5149, (short)5778, (short)6464, (short)7208, (short)7904, (short)8682, (short)9397, (short)10285, (short)11240, (short)12246, (short)13313, 
		(short)14382, (short)15492, (short)16735, (short)18131, (short)19693,(short)21280, (short)22912, (short)24624, (short)26544, (short)28432, (short)30488, (short)32720,
		/* 22 values in Q5 */
		(short)4383, (short)4684, (short)5012, (short)5363, (short)5739, (short)6146, (short)6603, (short)7113, (short)7679, (short)8285, (short)9040, (short)9850, (short)10838, 
		(short)11882, (short)13103, (short)14467, (short)15950, (short)17669, (short)19712, (short)22016, (short)24800, (short)28576,
		/* 5 values in Q3 */
		(short)8240, (short)9792, (short)12040, (short)15440, (short)22472
	};
	
	public static final short FILTER_RANGE[] = {
		(short)63, (short)85, (short)125, (short)147, (short)147
	};
	
	public static final short CB_FILTERS_REV[]={
		(short)-140, (short)446, (short)-755, (short)3302, (short)2922, (short)-590, (short)343, (short)-138
	};
	
	public static final short SEARCH_RANGE[][]={
		{(short)58,(short)58,(short)58}, {(short)108,(short)44,(short)44}, {(short)108,(short)108,(short)108}, {(short)108,(short)108,(short)108}, {(short)108,(short)108,(short)108}
	};
	
	public static final short GAIN_SQ3[]={
		(short)-16384, (short)-10813, (short)-5407, (short)0, (short)4096, (short)8192, (short)12288, (short)16384, (short)32767
	};
	
	public static final short GAIN_SQ4[]={
		(short)-17203, (short)-14746, (short)-12288, (short)-9830, (short)-7373, (short)-4915,(short)-2458, (short)0, (short)2458, (short)4915, 
		(short)7373, (short)9830, (short)12288, (short)14746, (short)17203, (short)19661, (short)32767
	};
	
	public static final short GAIN_SQ5[]={
		(short)614, (short)1229, (short)1843, (short)2458, (short)3072, (short)3686, (short)4301, (short)4915, (short)5530, (short)6144, 
		(short)6758, (short)7373, (short)7987, (short)8602, (short)9216, (short)9830, (short)10445, (short)11059, (short)11674, (short)12288, 
		(short)12902, (short)13517, (short)14131, (short)14746, (short)15360, (short)15974, (short)16589, (short)17203, (short)17818, 
		(short)18432, (short)19046, (short)19661, (short)32767
	};
	
	public static final short GAIN[][] = { GAIN_SQ5, GAIN_SQ4, GAIN_SQ3 };
	
	public static final short GAIN_SQ5_SQ[] = {
		(short)23, (short)92, (short)207, (short)368, (short)576, (short)829, (short)1129, (short)1474, (short)1866, (short)2304, (short)2787, (short)3317, (short)3893,
		(short)4516, (short)5184, (short)5897, (short)6658, (short)7464, (short)8318, (short)9216, (short)10160, (short)11151, (short)12187, (short)13271, (short)14400, (short)15574,
		(short)16796, (short)18062, (short)19377, (short)20736, (short)22140, (short)23593
	};
	
	public static final short ALPHA[]={ (short)6554, (short)13107, (short)19661, (short)26214 };
	
	public static final int CHOOSE_FRG_QUANT[] = { 118, 163, 222, 305, 425, 604, 851, 1174, 1617, 2222, 3080, 4191, 5525, 7215, 9193, 11540, 14397, 17604,
		21204, 25209, 29863, 35720, 42531, 50375, 59162, 68845, 80108, 93754, 110326, 129488, 150654, 174328, 201962, 233195, 267843, 308239, 354503, 405988, 
		464251, 531550, 608652, 697516, 802526, 928793, 1080145, 1258120, 1481106, 1760881, 2111111, 2546619, 3078825, 3748642, 4563142, 5573115, 6887601, 
		8582108, 10797296, 14014513, 18625760, 25529599, 37302935, 58819185, 109782723, Integer.MAX_VALUE
	};
	
	public static final short LSF_DIM_CB[]= { 
		(short)3, (short)3, (short)4 
	};
	
	public static final short LSF_SIZE_CB[] = {
		(short)64,(short)128,(short)128
	};
	
	public static final short LSF_INDEX_CB[]= { 
		(short)0, (short)192, (short)576 
	};
	
	public static final short ENG_START_SEQUENCE[]= {
		(short)1638, (short)1843, (short)2048, (short)1843, (short)1638
	};	
	
	public static final short PLC_PER_SQR[] = {
		(short)839, (short)1343, (short)2048, (short)2998, (short)4247, (short)5849
	};
	
	public static final short PLC_PITCH_FACT[] = {
		(short)0, (short)5462, (short)10922, (short)16384, (short)21846, (short)27306
	};
	
	public static final short PLC_PF_SLOPE[] = {
		(short)26667, (short)18729, (short)13653, (short)10258, (short)7901, (short)6214
	};
}
