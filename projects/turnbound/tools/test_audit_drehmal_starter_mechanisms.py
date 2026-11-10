import unittest
import audit_drehmal_starter_mechanisms as a

def state(id,face=None):
    return {"id":id,"facing":face,"conditional":False}
class MockWorld:
    def __init__(self,data): self.data=data
    def state(self,pos): return self.data.get(pos,state("minecraft:stone"))
class PhysicalTests(unittest.TestCase):
    def test_palette_bitpacking_across_word(self):
        p=[{"Name":str(i)} for i in range(17)]
        v=[0]*342
        for idx,val in ((0,16),(11,8),(12,9),(13,7),(4095,15)):
            v[idx//12]|=val<<((idx%12)*5)
        for idx,val in ((0,16),(11,8),(12,9),(13,7),(4095,15)):
            self.assertEqual(a.palette_index(p,tuple(v),idx),val)
    def test_bad_palette_rejected(self):
        p=[{"Name":"air"},{"Name":"stone"}]
        with self.assertRaises(ValueError): a.palette_index(p,None,20)
        with self.assertRaises(ValueError): a.palette_index(p,(255,),0)
    def test_turning_chain(self):
        w=MockWorld({(0,0,0):state("minecraft:command_block","east"),
                     (1,0,0):state("minecraft:chain_command_block","south"),
                     (1,0,1):state("minecraft:chain_command_block","east")})
        self.assertEqual(a.chain(w,(1,0,0)),(3,2,False))
    def test_nonchain_successor_not_counted(self):
        w=MockWorld({(0,0,0):state("minecraft:command_block","east"),
                     (1,0,0):state("minecraft:command_block","east")})
        self.assertEqual(a.chain(w,(0,0,0))[0],1)
        self.assertEqual(a.vicinity(w,(0,0,0))["adjacent_command"],1)
    def test_trigger_proximity(self):
        w=MockWorld({(0,0,0):state("minecraft:command_block","east"),
                     (0,1,0):state("minecraft:lever")})
        self.assertEqual(a.vicinity(w,(0,0,0))["signal"],1)
if __name__=="__main__": unittest.main()
