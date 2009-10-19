package com.mbien.opencl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import static com.mbien.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public class CLProgram {
    
    private final CLContext context;
    private final long programID;
    private final CL cl;
    
    public enum Status {
        
        BUILD_SUCCESS(CL.CL_BUILD_SUCCESS), 
        BUILD_NONE(CL.CL_BUILD_NONE), 
        BUILD_IN_PROGRESS(CL.CL_BUILD_IN_PROGRESS),
        BUILD_ERROR(CL.CL_BUILD_ERROR);
                
        /**
         * Value of wrapped OpenCL device type.
         */
        public final int CL_BUILD_STATUS;

        private Status(int CL_BUILD_STATUS) {
            this.CL_BUILD_STATUS = CL_BUILD_STATUS;
        }
        
        public static Status valueOf(int clBuildStatus) {
            switch(clBuildStatus) {
                case(CL.CL_BUILD_SUCCESS):
                    return BUILD_SUCCESS;
                case(CL.CL_BUILD_NONE):
                    return BUILD_NONE;
                case(CL.CL_BUILD_IN_PROGRESS):
                    return BUILD_IN_PROGRESS;
                case(CL.CL_BUILD_ERROR):
                    return BUILD_ERROR;
// is this a standard state?
//              case (CL.CL_BUILD_PROGRAM_FAILURE):
//                    return BUILD_PROGRAM_FAILURE;
            }
            return null;
        }
    }

    CLProgram(CLContext context, String src, long contextID) {
        
        this.cl = context.cl;
        this.context = context;

        int[] intArray = new int[1];
        // Create the program
        programID = cl.clCreateProgramWithSource(contextID, 1, new String[] {src}, new long[]{src.length()}, 0, intArray, 0);
        checkForError(intArray[0], "can not create program with source");
    }


    /**
     * Builds this program for all devices accosiated with the context and implementation specific build options.
     * @return this
     */
    public CLProgram build() {
        build(null, null);
        return this;
    }

    /**
     * Builds this program for the given devices and with the specified build options.
     * @return this
     * @param devices A list of devices this program should be build on or null for all devices of its context.
     */
    public CLProgram build(CLDevice[] devices, String options) {

        long[] deviceIDs = null;
        if(devices != null) {
            deviceIDs = new long[devices.length];
            for (int i = 0; i < deviceIDs.length; i++) {
                deviceIDs[i] = devices[i].deviceID;
            }
        }

        // Build the program
        int ret = cl.clBuildProgram(programID, deviceIDs, options, null, null);
        checkForError(ret, "error building program");

        return this;
    }

    /**
     * Releases this program.
     * @return this
     */
    public CLProgram release() {
        int ret = cl.clReleaseProgram(programID);
        checkForError(ret, "can not release program");
        context.programReleased(this);

        return this;
    }

    /**
     * Returns all devices associated with this program.
     */
    public CLDevice[] getCLDevices() {

        long[] longArray = new long[1];
        int ret = cl.clGetProgramInfo(programID, CL.CL_PROGRAM_DEVICES, 0, null, longArray, 0);
        checkForError(ret, "on clGetProgramInfo");

        ByteBuffer bb = ByteBuffer.allocate((int) longArray[0]).order(ByteOrder.nativeOrder());
        ret = cl.clGetProgramInfo(programID, CL.CL_PROGRAM_DEVICES, bb.capacity(), bb, null, 0);
        checkForError(ret, "on clGetProgramInfo");

        int count = bb.capacity() / 8; // TODO sizeof cl_device
        CLDevice[] devices = new CLDevice[count];
        for (int i = 0; i < count; i++) {
            devices[i] = context.getCLDevices(bb.getLong());
        }

        return devices;

    }

    public String getBuildLog(CLDevice device) {
        return getBuildInfoString(device.deviceID, CL.CL_PROGRAM_BUILD_LOG);
    }

    public Status getBuildStatus(CLDevice device) {
        int clStatus = getBuildInfoInt(device.deviceID, CL.CL_PROGRAM_BUILD_STATUS);
        return Status.valueOf(clStatus);
    }

    /**
     * Returns the source code of this program. Note: sources are not cached, each call of this method calls into OpenCL.
     */
    public String getSource() {
        return getProgramInfoString(CL.CL_PROGRAM_SOURCE);
    }

    // TODO binaries, serialization, program build options

    private final String getBuildInfoString(long device, int flag) {

        long[] longArray = new long[1];

        int ret = cl.clGetProgramBuildInfo(programID, device, flag, 0, null, longArray, 0);
        checkForError(ret, "on clGetProgramBuildInfo");

        ByteBuffer bb = ByteBuffer.allocate((int)longArray[0]).order(ByteOrder.nativeOrder());

        ret = cl.clGetProgramBuildInfo(programID, device, flag, bb.capacity(), bb, null, 0);
        checkForError(ret, "on clGetProgramBuildInfo");

        return new String(bb.array(), 0, (int)longArray[0]);
    }

    private final String getProgramInfoString(int flag) {

        long[] longArray = new long[1];

        int ret = cl.clGetProgramInfo(programID, flag, 0, null, longArray, 0);
        checkForError(ret, "on clGetProgramInfo");

        ByteBuffer bb = ByteBuffer.allocate((int)longArray[0]).order(ByteOrder.nativeOrder());

        ret = cl.clGetProgramInfo(programID, flag, bb.capacity(), bb, null, 0);
        checkForError(ret, "on clGetProgramInfo");

        return new String(bb.array(), 0, (int)longArray[0]);
    }

//    private int getProgramInfoInt(int flag) {
//
//        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
//
//        int ret = cl.clGetProgramInfo(programID, flag, bb.capacity(), bb, null, 0);
//        checkForError(ret, "");
//
//        return bb.getInt();
//    }
    
    private int getBuildInfoInt(long device, int flag) {

        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());

        int ret = cl.clGetProgramBuildInfo(programID, device, flag, bb.capacity(), bb, null, 0);
        checkForError(ret, "error on clGetProgramBuildInfo");

        return bb.getInt();
    }

}