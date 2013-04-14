/*******************************************************************************
 * Copyright (c) 2013, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * 
 * None of the name of the Regents of the University of California, or the names of its
 * contributors may be used to endorse or promote products derived from this software without specific
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package calico.utils.blobdetection;

//==================================================
//class MetaballsTable
//==================================================
public class MetaballsTable
{
	// Edge Cut Array
	// ------------------------------
	public static int edgeCut[][] =   
	{
		{-1, -1, -1, -1, -1}, //0
		{ 0,  3, -1, -1, -1}, //3
		{ 0,  1, -1, -1, -1}, //1
		{ 3,  1, -1, -1, -1}, //2
		{ 1,  2, -1, -1, -1}, //0
		{ 1,  2,  0,  3, -1}, //3
		{ 0,  2, -1, -1, -1}, //1
		{ 3,  2, -1, -1, -1}, //2
		{ 3,  2, -1, -1, -1}, //2
		{ 0,  2, -1, -1, -1}, //1
		{ 1,  2,  0,  3, -1}, //3
		{ 1,  2, -1, -1, -1}, //0
		{ 3,  1, -1, -1, -1}, //2
		{ 0,  1, -1, -1, -1}, //1
		{ 0,  3, -1, -1, -1}, //3
		{-1, -1, -1, -1, -1}  //0
	};
	
	// EdgeOffsetInfo Array
	// ------------------------------
	public static int edgeOffsetInfo[][] =  
	{
		{0,0,0},
		{1,0,1},
		{0,1,0},
		{0,0,1}
	
	};
	
	// EdgeToCompute Array
	// ------------------------------
	public static int edgeToCompute[] = {0,3,1,2,0,3,1,2,2,1,3,0,2,1,3,0};
	
	// neightborVoxel Array
	// ------------------------------
	// bit 0 : X+1
	// bit 1 : X-1
	// bit 2 : Y+1
	// bit 3 : Y-1
	public static byte neightborVoxel[] = {0,10,9,3,5,15,12,6,6,12,12,5,3,9,10,0};  
	public static void computeNeighborTable()
	{
	 int iEdge;
	 int n;
	 for (int i=0 ; i<16 ; i++)
	 {
	     neightborVoxel[i] = 0;
	
		n = 0;
		while ( (iEdge = MetaballsTable.edgeCut[i][n++]) != -1)
		{
	         switch (iEdge)
	         {
	           case 0:
	             neightborVoxel[i] |= (1<<3);     
	           break;
	           case 1:
	             neightborVoxel[i] |= (1<<0);     
	           break;
	           case 2:
	             neightborVoxel[i] |= (1<<2);     
	           break;
	           case 3:
	             neightborVoxel[i] |= (1<<1);     
	           break;
	         }
	     }    
	         
	 } // end for i
	
	 
	}

}
