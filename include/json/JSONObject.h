//-----------------------------------------------------------------------------------
// Copyright (c) 2009, Aventinus Ltd (aventinus.org). All rights reserved.
//-----------------------------------------------------------------------------------
#ifndef JSONOBJECT_INCL
#define JSONOBJECT_INCL

#include "TextString.h"
#include "Map.h"

//-----------------------------------------------------------------------------------
// The principal on errors (exceptions) here is that I can create an object from
//     an input string and see if it was valid or not - I am protected against the world.
// However, it I am constructing an object then we use abort() - I am not protected against myself
// Obviously in Java I can use Exceptions, in c++.....
//-----------------------------------------------------------------------------------

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
class JSONArray;
class JSONMap;
class JSONRequest;

class JSONObject
{
    friend class JSONArray;
    friend class JSONMap;

    public:
        JSONObject();
        JSONObject(const TextString& input);
        virtual ~JSONObject();
        JSONObject(const JSONObject& other);
        JSONObject& operator=(const JSONObject& other);

        static void verbose(int verbose) {m_verbose = verbose;}

        JSONObject& add(const TextString& name, const TextString& value);
        JSONObject& add(const TextString& name, int value);
        JSONObject& add(const TextString& name, long value);
        JSONObject& add(const TextString& name, double value);
        JSONObject& add(const TextString& name, const JSONObject& value);
        JSONObject& add(const TextString& name, const JSONArray& value);

        void removeValue(const TextString& name, TextString& target);
        void removeObject(const TextString& name, JSONObject& target);
        void removeArray(const TextString& name, JSONArray& target);

        const TextString& toString() const;

        JSONObject& startObject();
        JSONObject& endObject();
        JSONObject& startArray();
        JSONObject& endArray();
        JSONObject& key(const TextString& key);
        JSONObject& value(const TextString& value);
        JSONObject& value(int value);
        JSONObject& value(long value);
        JSONObject& value(double value);

        operator const char* () const;
        int contains(const TextString& name) const;
        TextString getString(const TextString& name) const;                                 // cannot return a ref due to escaping
        TextString getString(const TextString& name, const TextString& defaultValue) const;
        int getInteger(const TextString& name, int& target) const; 
        int getInteger(const TextString& name, int& target, int defaultValue) const; 
        int getLong(const TextString& name, long& target) const;
        int getLong(const TextString& name, long& target, long defaultValue) const;
        int getDouble(const TextString& name, double& target) const;
        int getDouble(const TextString& name, double& target, double defaultValue) const;
        JSONObject& getObject(const TextString& name, JSONObject& target) const;
        JSONArray& getArray(const TextString& name, JSONArray& target) const;
        JSONMap& getMap(JSONMap& target) const;
        List<TextString>& getNames(List<TextString>& target) const;

        const TextString& toString(const TextString& name) const;
        void normalise(JSONObject& target) const;

        int hasError() const                                   {return (m_error.length() > 0);}
        const TextString& getError() const                     {return m_error;}

        void dump() const;
        void dump(const TextString& text) const;
        void dump(TextString& buffer, int level) const;

        static TextString unescape(const TextString& value);
        static TextString escape(const TextString& value);

    protected:
        TextString m_error;

    private:
        void setError(const TextString& text, int current);

        JSONObject& rawValue(const TextString& value);
        JSONObject& addRaw(const TextString& name, const TextString& value);
        int getValue(const TextString& name, int type, TextString& target) const;

        void dumpStack() const;

        int length() const;
        char charAt(int index) const;
        TextString substring(int start, int end) const;
        int match(const JSONRequest& request, int start, int end) const;
        int parse2(int start, JSONRequest* request);
        int parse(int start, JSONRequest* request);
        void fixupRemove(int start, int end);

        TextString m_buffer;
        int m_start;
        int m_end;

        int* m_stack;
        int m_stackIdx;

        static int m_verbose;
};
#endif
